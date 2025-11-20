package com.historias_clinicas.hc.servicios;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaProcessorService {

    private final PlantillaRepository plantillaRepo;
    private final PlantillaSeccionRepository seccionRepo;
    private final PlantillaCampoRepository campoRepo;

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

        PlantillaSeccion seccion = PlantillaSeccion.builder()
                .nombre("SECCION_GENERAL")
                .plantilla(plantilla)
                .build();

        seccionRepo.save(seccion);

        detectarEtiquetas(doc, seccion);

        return Map.of("mensaje", "Plantilla analizada correctamente");
    }

    // ================================================================
    // 3. DETECTAR CAMPOS (PÁRRAFOS + TABLAS)
    // ================================================================
    private void detectarEtiquetas(XWPFDocument doc, PlantillaSeccion seccion) {

        int idxGlobalParrafo = 0;

        // -----------------------------
        // Fuera de tablas
        // -----------------------------
        for (XWPFParagraph p : doc.getParagraphs()) {

            procesarParrafo(p, seccion, idxGlobalParrafo, null, null, null);
            idxGlobalParrafo++;
        }

        // -----------------------------
        // Dentro de tablas
        // -----------------------------
        int idxTabla = 0;
        for (XWPFTable tabla : doc.getTables()) {

            boolean esMatriz = detectarTablaEstructurada(tabla, seccion, idxTabla);

            // Si es tabla estructurada → NO procesar como párrafos normales
            if (esMatriz) {
                idxTabla++;
                continue;
            }

            // Si NO es matriz → procesar normalmente
            int idxFila = 0;
            for (XWPFTableRow fila : tabla.getRows()) {

                int idxCelda = 0;
                for (XWPFTableCell celda : fila.getTableCells()) {

                    for (XWPFParagraph p : celda.getParagraphs()) {

                        procesarParrafo(
                                p,
                                seccion,
                                idxGlobalParrafo,
                                idxTabla,
                                idxFila,
                                idxCelda
                        );
                        idxGlobalParrafo++;
                    }
                    idxCelda++;
                }
                idxFila++;
            }
            idxTabla++;
        }
    }

    // ================================================================
    // 4. DETECTAR Y GUARDAR ETIQUETAS DE PÁRRAFOS
    // ================================================================
    private void procesarParrafo(
            XWPFParagraph p,
            PlantillaSeccion seccion,
            Integer idxParrafo,
            Integer idxTabla,
            Integer idxFila,
            Integer idxCelda
    ) {

        if (p == null || p.getRuns() == null || p.getRuns().isEmpty()) return;

        List<XWPFRun> runs = p.getRuns();

        for (int i = 0; i < runs.size(); i++) {

            String text = runs.get(i).toString().trim();

            if (text.isBlank()) continue;

            if (esEtiquetaCampo(text)) {

                String etiqueta = text;
                String nombreCampo = etiqueta.replace(":", "")
                        .trim()
                        .toLowerCase();

                PlantillaCampo campo = PlantillaCampo.builder()
                        .seccion(seccion)
                        .nombreCampo(nombreCampo)
                        .textoOriginal(etiqueta)
                        .indexParrafo(idxParrafo)
                        .indexTabla(idxTabla)
                        .indexFila(idxFila)
                        .indexCelda(idxCelda)
                        .indexRunInicio(i)
                        .indexRunFin(i)
                        .build();

                campoRepo.save(campo);

                log.info("Campo detectado: {} en párrafo {}", nombreCampo, idxParrafo);
            }
        }
    }

    // ================================================================
    // 5. DETECTAR TABLAS ESTRUCTURADAS
    // ================================================================
    private boolean detectarTablaEstructurada(XWPFTable tabla, PlantillaSeccion seccion, int idxTabla) {

        List<XWPFTableRow> filas = tabla.getRows();
        if (filas == null || filas.size() < 2) return false;

        // Leer encabezados
        XWPFTableRow header = filas.get(0);
        List<XWPFTableCell> headerCeldas = header.getTableCells();

        if (headerCeldas.size() < 2) return false;

        List<String> columnas = new ArrayList<>();

        for (XWPFTableCell cell : headerCeldas) {

            String titulo = cell.getText();
            if (titulo == null) return false;

            titulo = titulo.replace("\n", " ")
                    .replace("\r", "")
                    .trim()
                    .toLowerCase();

            // encabezado debe ser texto simple, NO adornos
            if (!titulo.matches("^[a-záéíóúüñ ]+$"))
                return false;

            columnas.add(titulo);
        }

        // Procesar filas
        for (int f = 1; f < filas.size(); f++) {

            XWPFTableRow fila = filas.get(f);
            List<XWPFTableCell> celdas = fila.getTableCells();

            if (celdas.isEmpty()) continue;

            String filaLabel = celdas.get(0).getText();
            if (filaLabel == null) continue;

            filaLabel = filaLabel.replace("\n", " ")
                    .replace("\r", "")
                    .trim()
                    .toLowerCase();

            if (!filaLabel.matches("^[a-záéíóúüñ ]+$")) continue;

            for (int c = 1; c < columnas.size(); c++) {

                if (c >= celdas.size()) break;

                XWPFTableCell celda = celdas.get(c);
                List<XWPFParagraph> parrafos = celda.getParagraphs();

                if (parrafos == null || parrafos.isEmpty()) continue;

                for (int p = 0; p < parrafos.size(); p++) {

                    XWPFParagraph parrafo = parrafos.get(p);
                    List<XWPFRun> runs = parrafo.getRuns();

                    if (runs == null || runs.isEmpty()) continue;

                    for (int r = 0; r < runs.size(); r++) {

                        PlantillaCampo campo = PlantillaCampo.builder()
                                .seccion(seccion)
                                .nombreCampo(filaLabel + "." + columnas.get(c))
                                .textoOriginal(columnas.get(c))
                                .indexTabla(idxTabla)
                                .indexFila(f)
                                .indexCelda(c)
                                .indexParrafo(null)
                                .indexRunInicio(r)
                                .indexRunFin(r)
                                .build();

                        campoRepo.save(campo);
                    }
                }
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
    // 7. REGEX DE ETIQUETAS
    // ================================================================
    private boolean esEtiquetaCampo(String texto) {
        if (texto == null) return false;
        texto = texto.trim();
        return texto.matches("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 ]+:$");
    }
}