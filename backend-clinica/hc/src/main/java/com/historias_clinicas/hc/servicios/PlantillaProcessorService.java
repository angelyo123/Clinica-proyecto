package com.historias_clinicas.hc.servicios;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.entidades.Plantilla;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.entidades.PlantillaSeccion;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.repositorios.PlantillaRepository;
import com.historias_clinicas.hc.repositorios.PlantillaSeccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaProcessorService {

    private final PlantillaRepository plantillaRepo;
    private final PlantillaSeccionRepository seccionRepo;
    private final PlantillaCampoRepository campoRepo;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService iaExecutor;

    // ------------------------------------------------------
    // SUBIR PLANTILLA
    // ------------------------------------------------------
    public Plantilla procesarPlantilla(byte[] file, String nombre) {
        Plantilla plantilla = Plantilla.builder()
                .nombre(nombre)
                .archivoOriginal(file)
                .build();

        plantillaRepo.save(plantilla);
        return plantilla;
    }

    // ------------------------------------------------------
    // ANALIZAR PLANTILLA
    // ------------------------------------------------------
    @Transactional
    public Map<String, Object> analizarPlantilla(Long plantillaId) throws Exception {

        Plantilla plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(plantilla.getArchivoOriginal()));

        limpiarEstructuraAnterior(plantilla);

        // Paso 1: Extraer estructura física SIN procesar items
        Map<String, Object> estructura = extraerEstructuraCompacta(doc);

        // Paso 2: Interpretación IA por bloques
        Map<String, Object> interpretacion = interpretarPorBloquesParalelo(estructura);

        // Paso 3: Crear campos desde IA
        crearCamposDesdeIA(interpretacion, plantilla);

        return Map.of(
                "estructuraPOI", estructura,
                "estructuraIA", interpretacion
        );
    }


    // =====================================================================
    // NUEVO EXTRACTOR POI (solo texto bruto)
    // =====================================================================
    private Map<String, Object> extraerEstructuraCompacta(XWPFDocument doc) {

        Map<String, Object> data = new LinkedHashMap<>();

        // ------------------------------------------------------
        // 1) PÁRRAFOS
        // ------------------------------------------------------
        List<Map<String, Object>> parrafos = new ArrayList<>();
        int idxP = 0;

        for (XWPFParagraph p : doc.getParagraphs()) {
            String texto = p.getText();
            if (texto != null) texto = texto.trim();

            if (texto != null && !texto.isEmpty()) {
                parrafos.add(Map.of(
                        "tipo", "parrafo",
                        "indexParrafo", idxP,
                        "texto", texto
                ));
            }
            idxP++;
        }

        data.put("parrafos", parrafos);


        // ------------------------------------------------------
        // 2) TABLAS → FILAS → CELDAS
        // ------------------------------------------------------
        List<Map<String, Object>> tablas = new ArrayList<>();
        int idxTabla = 0;

        for (XWPFTable tabla : doc.getTables()) {

            List<Map<String, Object>> filas = new ArrayList<>();
            int idxFila = 0;

            // 🔥 PRIMERA PASADA → obtener número máximo de columnas de la tabla
            int maxCols = 0;
            for (XWPFTableRow r : tabla.getRows()) {
                maxCols = Math.max(maxCols, r.getTableCells().size());
            }

            // 🔥 SEGUNDA PASADA → reconstruir filas con columnas vacías
            for (XWPFTableRow row : tabla.getRows()) {

                List<Map<String, Object>> celdas = new ArrayList<>();

                // celdas reales que POI sí detectó
                List<XWPFTableCell> realCells = row.getTableCells();

                for (int c = 0; c < maxCols; c++) {

                    XWPFTableCell celda = (c < realCells.size() ? realCells.get(c) : null);

                    String textoCelda =
                            (celda != null ? celda.getText() : "")
                                    .replace("\n", "")
                                    .trim();

                    Map<String, Object> celdaMap = new LinkedHashMap<>();
                    celdaMap.put("tipo", "celda");
                    celdaMap.put("tabla", idxTabla);
                    celdaMap.put("fila", idxFila);
                    celdaMap.put("columna", c);
                    celdaMap.put("texto", textoCelda);

                    // METADATA UNIVERSAL (útil para IA)
                    celdaMap.put("isAllCaps", textoCelda.equals(textoCelda.toUpperCase()));
                    celdaMap.put("wordCount", textoCelda.isEmpty() ? 0 : textoCelda.split("\\s+").length);
                    celdaMap.put("hasColon", textoCelda.contains(":"));
                    celdaMap.put("hasParenthesis", textoCelda.contains("("));
                    celdaMap.put("isSingleCellRow", maxCols == 1);
                    celdaMap.put("rowCellCount", maxCols);

                    celdas.add(celdaMap);
                }

                filas.add(
                        Map.of(
                                "fila", idxFila,
                                "celdas", celdas
                        )
                );

                idxFila++;
            }

            tablas.add(
                    Map.of(
                            "tabla", idxTabla,
                            "filas", filas
                    )
            );

            idxTabla++;
        }

        data.put("tablas", tablas);
        return data;
    }



    // =====================================================================
    // INTERPRETAR POR BLOQUES (igual que antes, pero ahora con celdas simples)
    // =====================================================================
    private Map<String, Object> interpretarPorBloquesParalelo(Map<String, Object> estructura) throws Exception {

        List<Map<String, Object>> statsBloques = new ArrayList<>();
        List<Map<String, Object>> bloques = dividirEnBloques(estructura, 8000);

        Map<Integer, String> respuestasParciales = new ConcurrentHashMap<>();

        log.warn("🟥 Procesando {} bloques IA…", bloques.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int index = 0;

        for (Map<String, Object> bloque : bloques) {

            final int bloqueId = ++index;

            CompletableFuture<Void> future =
                    CompletableFuture.runAsync(() -> {

                        try {
                            String entrada = mapper.writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(bloque);

                            long inicio = System.currentTimeMillis();

                            String raw = deepSeekClient.completarJSON_sinValidar(bloque, DeepSeekClient.IA_FIELD_ANALYZER_PROMPT);

                            long tiempo = System.currentTimeMillis() - inicio;

                            String reparado = repararMegaJSON(raw);

                            respuestasParciales.put(bloqueId, reparado);

                            statsBloques.add(Map.of(
                                    "bloqueId", bloqueId,
                                    "chars", entrada.length(),
                                    "tiempo_ms", tiempo,
                                    "estado", "OK"
                            ));

                        } catch (Exception e) {
                            respuestasParciales.put(bloqueId, "{}");
                        }

                    }, iaExecutor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Map<String, Object> resultadoFinal = new LinkedHashMap<>();

        for (Integer bloqueId : respuestasParciales.keySet()) {

            String json = respuestasParciales.get(bloqueId);

            try {
                Map<String, Object> parsed = mapper.readValue(json, Map.class);
                resultadoFinal.putAll(parsed);
            } catch (Exception ignored) {}
        }

        resultadoFinal.put("_statsBloques", statsBloques);
        return resultadoFinal;
    }



    // =====================================================================
    // JSON FIXER
    // =====================================================================
    private String repararMegaJSON(String raw) {
        if (raw == null || raw.isBlank()) return "{}";

        String txt = raw.trim();

        txt = txt.replaceAll("^[^{]+", "");
        txt = txt.replaceAll("[^}]+$", "");

        if (!txt.startsWith("{")) txt = "{" + txt;
        if (!txt.endsWith("}")) txt = txt + "}";

        long count = txt.chars().filter(c -> c == '"').count();
        if (count % 2 != 0) txt += "\"";

        txt = txt.replaceAll(",\\s*}", "}");

        return balancearLlaves(txt);
    }

    private String balancearLlaves(String s) {
        int open = 0, close = 0;

        for (char c : s.toCharArray()) {
            if (c == '{') open++;
            if (c == '}') close++;
        }

        while (close < open) {
            s += "}";
            close++;
        }
        return s;
    }



    // =====================================================================
    // DIVISOR DE BLOQUES IA (igual que antes)
    // =====================================================================
    private List<Map<String, Object>> dividirEnBloques(Map<String, Object> estructura, int maxChars) throws Exception {

        List<Object> items = new ArrayList<>();
        items.addAll((List<?>) estructura.get("parrafos"));

        List<Map<String, Object>> tablas = (List<Map<String, Object>>) estructura.get("tablas");

        for (Map<String, Object> tabla : tablas) {

            int tablaId = (int) tabla.get("tabla");

            for (Map<String, Object> fila : (List<Map<String, Object>>) tabla.get("filas")) {

                int filaId = (int) fila.get("fila");

                Map<String, Object> filaCompacta = new LinkedHashMap<>();
                filaCompacta.put("tabla", tablaId);
                filaCompacta.put("fila", filaId);
                filaCompacta.put("celdas", fila.get("celdas"));

                items.add(filaCompacta);
            }
        }

        List<Map<String, Object>> bloques = new ArrayList<>();
        List<Object> actual = new ArrayList<>();
        int sizeActual = 0;

        for (Object item : items) {
            String json = mapper.writeValueAsString(item);
            int size = json.length();

            if (sizeActual + size > maxChars) {
                bloques.add(Map.of("items", new ArrayList<>(actual)));
                actual.clear();
                sizeActual = 0;
            }

            actual.add(item);
            sizeActual += size;
        }

        if (!actual.isEmpty()) bloques.add(Map.of("items", actual));

        return bloques;
    }



    // =====================================================================
    // CREAR CAMPOS DESDE IA (NUEVO MODELO)
    // =====================================================================
    private void crearCamposDesdeIA(Map<String, Object> ia, Plantilla plantilla) {

        PlantillaSeccion seccion = PlantillaSeccion.builder()
                .nombre("SECCION_GENERAL")
                .plantilla(plantilla)
                .build();

        seccionRepo.save(seccion);

        for (String key : ia.keySet()) {

            if (key.startsWith("_")) continue;

            Map<String, Object> info = (Map<String, Object>) ia.get(key);

            Integer tabla = (Integer) info.get("tabla");
            Integer fila = (Integer) info.get("fila");
            Integer col = (Integer) info.get("columna");
            Integer itemIndex = (Integer) info.get("itemIndex");

            String textoOriginal = (String) info.get("textoOriginal");
            String tipo = (String) info.get("tipo");
            String descripcion = (String) info.get("descripcion");

            String nombreCampoFinal = generarNombreCampo(textoOriginal, tabla, fila, col, itemIndex);

            PlantillaCampo campo = PlantillaCampo.builder()
                    .seccion(seccion)
                    .nombreCampo(nombreCampoFinal)
                    .textoOriginal(textoOriginal)
                    .tipoCampo(tipo)
                    .descripcionCampo(descripcion)
                    .indexTabla(tabla)
                    .indexFila(fila)
                    .indexCelda(col)
                    .itemIndex(itemIndex)
                    .indexParrafo(null)
                    .build();

            campoRepo.save(campo);
        }
    }


    private String generarNombreCampo(String textoOriginal, int tabla, int fila, int col, int itemIndex) {

        if (textoOriginal == null) textoOriginal = "campo";

        String t = textoOriginal;

        t = t.replace("( )", "").replace("()", "").trim();

        t = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        t = t.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        return t +
                "_t" + tabla +
                "_f" + fila +
                "_c" + col +
                "_i" + itemIndex;
    }


    // =====================================================================
    // LIMPIAR BD
    // =====================================================================
    private void limpiarEstructuraAnterior(Plantilla plantilla) {

        List<PlantillaSeccion> secs = seccionRepo.findByPlantillaId(plantilla.getId());

        for (PlantillaSeccion s : secs) {
            campoRepo.deleteBySeccionId(s.getId());
        }

        seccionRepo.deleteAll(secs);
    }
}
