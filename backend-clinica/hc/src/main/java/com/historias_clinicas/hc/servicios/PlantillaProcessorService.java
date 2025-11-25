package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.dto.CeldaAnalizadaDTO;
import com.historias_clinicas.hc.dto.PalabraDTO;
import com.historias_clinicas.hc.entidades.Plantilla;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.entidades.PlantillaSeccion;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.repositorios.PlantillaRepository;
import com.historias_clinicas.hc.repositorios.PlantillaSeccionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaProcessorService {

    private final PlantillaRepository plantillaRepo;
    private final PlantillaSeccionRepository seccionRepo;
    private final PlantillaCampoRepository campoRepo;

    // ⭐ Lista final para enviar a DeepSeek
    private final List<CeldaAnalizadaDTO> listaCeldasParaDeepSeek = new ArrayList<>();

    public List<CeldaAnalizadaDTO> getListaCeldasParaDeepSeek() {
        return listaCeldasParaDeepSeek;
    }

    // ================================================================
    // 1. SUBIR PLANTILLA
    // ================================================================
    public Plantilla procesarPlantilla(MultipartFile file, String nombre) throws Exception {

        Plantilla plantilla = Plantilla.builder()
                .nombre(nombre)
                .archivoOriginal(file.getBytes())
                .build();

        plantillaRepo.save(plantilla);
        return plantilla;
    }

    // ================================================================
    // 2. ANALIZAR Y GENERAR ESTRUCTURA
    // ================================================================
    @Transactional
    public Map<String, Object> analizarPlantilla(Long plantillaId) throws Exception {

        Plantilla plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal())
        );

        limpiarEstructuraAnterior(plantilla);
        listaCeldasParaDeepSeek.clear();

        PlantillaSeccion seccion = PlantillaSeccion.builder()
                .nombre("SECCION_GENERAL")
                .plantilla(plantilla)
                .build();

        seccionRepo.save(seccion);

        detectarEtiquetas(doc, seccion);

        return Map.of(
                "mensaje", "Plantilla analizada correctamente",
                "celdas", listaCeldasParaDeepSeek
        );
    }

    // ================================================================
    // 3. DETECTAR CAMPOS (PÁRRAFOS + TABLAS)
    // ================================================================
    private void detectarEtiquetas(XWPFDocument doc, PlantillaSeccion seccion) {

        int idxGlobalParrafo = 0;

        // ----------------------------------------------------
        // PÁRRAFOS FUERA DE TABLAS
        // ----------------------------------------------------
        for (XWPFParagraph p : doc.getParagraphs()) {
            procesarParrafo(p, seccion, idxGlobalParrafo, null, null, null);
            idxGlobalParrafo++;
        }

        // ----------------------------------------------------
        // TABLAS
        // ----------------------------------------------------
        int idxTabla = 0;

        for (XWPFTable tabla : doc.getTables()) {

            boolean esMatriz = detectarTablaEstructurada(tabla, seccion, idxTabla);

            if (!esMatriz) {

                int idxFila = 0;

                for (XWPFTableRow fila : tabla.getRows()) {

                    int idxCelda = 0;

                    for (XWPFTableCell celda : fila.getTableCells()) {

                        // ==========================================================
                        // ⭐ DETECCIÓN MEJORADA PARA PA, FC, T°, FR, SO2, FIO2, etc.
                        // ==========================================================
                        String raw = celda.getText();
                        if (raw != null) {

                            // Normalizar contenido
                            String texto = raw.replace("\n", " ")
                                    .replace("\r", " ")
                                    .trim();

                            // ---- Nuevo: dividir textos complejos ----
                            // Ejemplo: "Tº : C°"  → ["Tº", ":", "C°"]
                            String[] tokens = texto.split("\\s+");

                            for (String token : tokens) {
                                if (token.isBlank()) continue;

                                String limpio = token.replace(":", "").trim();

                                // Detectar PA, FC, FR, T°, Temp, etc.
                                if (esEtiquetaVital(limpio)) {

                                    String etiquetaNormalizada = normalizarEtiquetaVital(limpio);

                                    // Validar patrón multicelda:
                                    boolean derechaEsDosPuntos = false;
                                    boolean derechaEsUnidad = false;

                                    // Celda derecha
                                    if (idxCelda + 1 < fila.getTableCells().size()) {
                                        String derecha = fila.getCell(idxCelda + 1).getText()
                                                .replace("\n", " ").replace("\r", " ").trim();

                                        if (derecha.equals(":")) derechaEsDosPuntos = true;
                                        if (derecha.matches("(?i)^(c°|cº|%|kg|m)$"))
                                            derechaEsUnidad = true;
                                    }

                                    // Dos celdas a la derecha
                                    boolean dosDerechaUnidad = false;
                                    if (idxCelda + 2 < fila.getTableCells().size()) {
                                        String dd = fila.getCell(idxCelda + 2).getText()
                                                .replace("\n", " ").replace("\r", " ").trim();

                                        if (dd.matches("(?i)^(c°|cº|%|kg|m)$"))
                                            dosDerechaUnidad = true;
                                    }

                                    // Caso válido: etiqueta + ":" o etiqueta + unidad (C°, %, etc.)
                                    if (derechaEsDosPuntos || derechaEsUnidad || dosDerechaUnidad) {

                                        String nombreCampo = generarNombreUnico(
                                                seccion.getNombre(),
                                                etiquetaNormalizada,
                                                idxGlobalParrafo,
                                                idxTabla,
                                                idxFila,
                                                idxCelda
                                        );

                                        PlantillaCampo campo = PlantillaCampo.builder()
                                                .seccion(seccion)
                                                .nombreCampo(nombreCampo)
                                                .textoOriginal(etiquetaNormalizada)
                                                .indexParrafo(idxGlobalParrafo)
                                                .indexTabla(idxTabla)
                                                .indexFila(idxFila)
                                                .indexCelda(idxCelda)
                                                .build();

                                        campoRepo.save(campo);
                                        log.warn("✔️ Campo VITAL detectado: {} (ID={})",
                                                nombreCampo, campo.getId());
                                    }
                                }
                            }
                        }

                        // ==========================================================
                        // ⭐ Enviar celda completa a DeepSeek
                        // ==========================================================
                        CeldaAnalizadaDTO celdaDTO = analizarCelda(celda, idxTabla, idxFila, idxCelda);
                        listaCeldasParaDeepSeek.add(celdaDTO);

                        // ---------------------------------------------------------
                        // Etiquetas dentro de párrafos de celdas
                        // ---------------------------------------------------------
                        for (XWPFParagraph p : celda.getParagraphs()) {
                            procesarParrafo(p, seccion, idxGlobalParrafo,
                                    idxTabla, idxFila, idxCelda);
                            idxGlobalParrafo++;
                        }

                        idxCelda++;
                    }

                    idxFila++;
                }
            }

            idxTabla++;
        }
    }

    private boolean esEtiquetaVital(String e) {
        if (e == null) return false;
        e = e.toLowerCase().trim();

        return e.matches("pa|fc|fr|t[º°]?|temp|so2|sao2|spo2|so|fio2|fio");
    }


    private String normalizarEtiquetaVital(String e) {
        if (e == null) return null;
        e = e.toLowerCase().trim();

        if (e.matches("t[º°]?") || e.equals("temp"))
            return "temperatura";

        if (e.equals("pa")) return "presion_arterial";
        if (e.equals("fc")) return "frecuencia_cardiaca";
        if (e.equals("fr")) return "frecuencia_respiratoria";

        if (e.matches("(so2|sao2|spo2|so)"))
            return "saturacion";

        if (e.matches("fio2|fio"))
            return "fio2";

        return e;
    }


    private String generarNombreUnico(
            String seccion,
            String etiqueta,
            Integer p,
            Integer t,
            Integer f,
            Integer c
    ) {

        String base = (seccion + "_" + etiqueta)
                .toLowerCase()
                .replace(":", "")
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");

        StringBuilder suf = new StringBuilder();
        if (t != null) suf.append("_t").append(t);
        if (f != null) suf.append("_f").append(f);
        if (c != null) suf.append("_c").append(c);
        if (p != null) suf.append("_p").append(p);

        return base + suf;
    }

    // ================================================================
    // 4. DETECTAR ETIQUETAS DE PÁRRAFOS
    // ================================================================
    private void procesarParrafo(
            XWPFParagraph p,
            PlantillaSeccion seccion,
            Integer idxParrafo,
            Integer idxTabla,
            Integer idxFila,
            Integer idxCelda
    ) {

        if (p == null || p.getRuns() == null || p.getRuns().isEmpty())
            return;

        // 1. Unir TODOS los runs en un solo texto
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : p.getRuns()) {
            sb.append(run.text()).append(" ");
        }
        String textoCompleto = sb.toString().trim();

        if (textoCompleto.isBlank())
            return;

        // 2. Normalizar espacios
        String normal = textoCompleto
                .replaceAll("\\s+", " ")       // compactar espacios
                .replaceAll("_+", "")          // eliminar subrayados
                .trim();

        // 3. Detectar etiqueta si empieza con palabra + opcional ":" o espacios
        //   Ejemplos detectados:
        //   - "Edad:"
        //   - "Edad :"
        //   - "Edad       :"
        //   - "Edad"
        //   - "Sexo :"
        //   - "Raza"
        //   - "Estado Civil :"
        //   - "Grado de instrucción"
        //   - "Procedencia :"
        //
        //   SOLO SI la etiqueta está al inicio del párrafo o celda
        Pattern pattern = Pattern.compile("^([A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+?)\\s*:?$");
        Matcher m = pattern.matcher(normal);

        if (m.find()) {

            String etiqueta = m.group(1).trim() + ":"; // uniformizar

            String nombreCampo = generarNombreUnico(
                    seccion.getNombre(),
                    etiqueta,
                    idxParrafo,
                    idxTabla,
                    idxFila,
                    idxCelda
            );

            PlantillaCampo campo = PlantillaCampo.builder()
                    .seccion(seccion)
                    .nombreCampo(nombreCampo)
                    .textoOriginal(etiqueta)
                    .indexParrafo(idxParrafo)
                    .indexTabla(idxTabla)
                    .indexFila(idxFila)
                    .indexCelda(idxCelda)
                    .indexRunInicio(0)
                    .indexRunFin(p.getRuns().size() - 1)
                    .build();

            campoRepo.save(campo);

            log.warn("✔️ Campo detectado: {} (ID={})", nombreCampo, campo.getId());
        }
    }

    // ================================================================
    // 4B. ANALIZAR CELDA COMPLETA (DeepSeek-ready)
    // ================================================================
    private CeldaAnalizadaDTO analizarCelda(
            XWPFTableCell celda,
            int idxTabla,
            int idxFila,
            int idxColumna
    ) {
        List<PalabraDTO> palabras = new ArrayList<>();
        StringBuilder textoCompleto = new StringBuilder();

        int wordIndex = 0;

        for (XWPFParagraph p : celda.getParagraphs()) {

            List<XWPFRun> runs = p.getRuns();
            if (runs == null) continue;

            for (int r = 0; r < runs.size(); r++) {

                String text = runs.get(r).text();
                if (text == null) continue;

                textoCompleto.append(text).append(" ");

                String[] separadas = text.split("\\s+");

                for (String palabra : separadas) {
                    if (palabra.isBlank()) continue;

                    palabras.add(
                            PalabraDTO.builder()
                                    .texto(palabra)
                                    .runIndex(r)
                                    .wordIndex(wordIndex++)
                                    .build()
                    );
                }
            }
        }

        return CeldaAnalizadaDTO.builder()
                .tabla(idxTabla)
                .fila(idxFila)
                .columna(idxColumna)
                .textoCompleto(textoCompleto.toString().trim())
                .palabras(palabras)
                .build();
    }

    // ================================================================
    // 5. DETECTAR TABLAS ESTRUCTURADAS
    // ================================================================
    private boolean detectarTablaEstructurada(XWPFTable tabla, PlantillaSeccion seccion, int idxTabla) {

        List<XWPFTableRow> filas = tabla.getRows();
        if (filas == null || filas.size() < 2) return false;

        XWPFTableRow header = filas.get(0);
        List<XWPFTableCell> headerCeldas = header.getTableCells();

        if (headerCeldas.size() < 2) return false;

        List<String> columnas = new ArrayList<>();

        for (XWPFTableCell cell : headerCeldas) {
            String titulo = cell.getText();
            if (titulo == null) return false;

            titulo = titulo.replace("\n", " ").replace("\r", "").trim().toLowerCase();

            if (!titulo.matches("^[a-záéíóúüñ ]+$"))
                return false;

            columnas.add(titulo);
        }

        for (int f = 1; f < filas.size(); f++) {

            XWPFTableRow fila = filas.get(f);
            List<XWPFTableCell> celdas = fila.getTableCells();
            if (celdas.isEmpty()) continue;

            String filaLabel = celdas.get(0).getText();
            if (filaLabel == null) continue;

            filaLabel = filaLabel.replace("\n", " ").replace("\r", "").trim().toLowerCase();
            if (!filaLabel.matches("^[a-záéíóúüñ ]+$")) continue;

            for (int c = 1; c < columnas.size(); c++) {

                if (c >= celdas.size()) break;

                XWPFTableCell celda = celdas.get(c);

                PlantillaCampo campo = PlantillaCampo.builder()
                        .seccion(seccion)
                        .nombreCampo(filaLabel + "." + columnas.get(c))
                        .textoOriginal(columnas.get(c))
                        .indexTabla(idxTabla)
                        .indexFila(f)
                        .indexCelda(c)
                        .build();

                campoRepo.save(campo);
            }
        }

        log.info("Tabla estructurada detectada en índice {}", idxTabla);
        return true;
    }

    // ================================================================
    // 6. LIMPIAR ESTRUCTURA ANTERIOR
    // ================================================================
    private void limpiarEstructuraAnterior(Plantilla plantilla) {

        List<PlantillaSeccion> secs = seccionRepo.findByPlantillaId(plantilla.getId());

        for (PlantillaSeccion sec : secs) {
            campoRepo.deleteBySeccionId(sec.getId());
        }

        seccionRepo.deleteAll(secs);
    }

    // ================================================================
    // 7. REGEX DE ETIQUETAS "Campo:"
    // ================================================================
    private boolean esEtiquetaCampo(String texto) {
        if (texto == null) return false;
        texto = texto.trim();
        return texto.matches("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+:?$");
    }


}
