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

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal())
        );

        limpiarEstructuraAnterior(plantilla);

        // 1. Extraer estructura compactada
        Map<String, Object> estructura = extraerEstructuraCompacta(doc);

        // 2. Interpretar por bloques en paralelo
        Map<String, Object> interpretacion = interpretarPorBloquesParalelo(estructura);

        // 3. Crear campos en BD
        crearCamposDesdeIA(interpretacion, plantilla);

        return Map.of(
                "estructuraPOI", estructura,
                "estructuraIA", interpretacion
        );
    }

    // ------------------------------------------------------
    // EXTRAER ESTRUCTURA COMPACTA (OPTIMIZADO)
    // ------------------------------------------------------
    private Map<String, Object> extraerEstructuraCompacta(XWPFDocument doc) {

        Map<String, Object> data = new LinkedHashMap<>();

        List<Map<String, Object>> parrafos = new ArrayList<>();
        int idxP = 0;

        for (XWPFParagraph p : doc.getParagraphs()) {
            String texto = p.getText().trim();
            if (texto.isEmpty()) continue; // quitar vacíos

            parrafos.add(Map.of(
                    "indexParrafo", idxP,
                    "texto", texto
            ));
            idxP++;
        }

        data.put("parrafos", parrafos);

        // Tablas compactadas
        List<Map<String, Object>> tablas = new ArrayList<>();
        int idxTabla = 0;

        for (XWPFTable tabla : doc.getTables()) {

            List<Map<String, Object>> filas = new ArrayList<>();
            int idxFila = 0;

            for (XWPFTableRow row : tabla.getRows()) {

                List<Map<String, Object>> celdas = new ArrayList<>();
                int idxCelda = 0;

                for (XWPFTableCell celda : row.getTableCells()) {

                    String texto = celda.getText();
                    if (texto == null || texto.trim().isEmpty()) {
                        idxCelda++;
                        continue;
                    }

                    celdas.add(Map.of(
                            "columna", idxCelda,
                            "textoCompleto", texto,
                            "palabras", List.of(texto.split("\\s+"))
                    ));

                    idxCelda++;
                }

                if (!celdas.isEmpty()) {
                    filas.add(Map.of(
                            "fila", idxFila,
                            "celdas", celdas
                    ));
                }

                idxFila++;
            }

            if (!filas.isEmpty()) {
                tablas.add(Map.of(
                        "tabla", idxTabla,
                        "filas", filas
                ));
            }

            idxTabla++;
        }

        data.put("tablas", tablas);

        return data;
    }

    // ------------------------------------------------------
    // BLOQUES EN PARALELO (15k chars)
    // ------------------------------------------------------
    private Map<String, Object> interpretarPorBloquesParalelo(Map<String, Object> estructura) throws Exception {

        List<Map<String, Object>> statsBloques = new ArrayList<>();

        List<Map<String, Object>> bloques = dividirEnBloques(estructura, 8000);

        Map<Integer, String> respuestasParciales = new ConcurrentHashMap<>();

        log.warn("🟥 Iniciando análisis en paralelo: {} bloques", bloques.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int index = 0;

        for (Map<String, Object> bloque : bloques) {

            final int bloqueId = ++index;
            final String bloqueJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bloque);
            final int chars = bloqueJson.length();

            CompletableFuture<Void> future =
                    CompletableFuture.runAsync(() -> {

                        long inicio = System.currentTimeMillis();
                        String thread = Thread.currentThread().getName();

                        // LOG DEL BLOQUE ENVIADO
                        log.warn("\n" +
                                        "─────────────────────────────────────────────\n" +
                                        "🟦 BLOQUE {} ENVIADO\n" +
                                        "Hilo: {}\n" +
                                        "Tamaño: {} chars\n" +
                                        "Contenido enviado a DeepSeek:\n{}\n" +
                                        "─────────────────────────────────────────────",
                                bloqueId, thread, chars, bloqueJson
                        );

                        try {
                            String raw = deepSeekClient.completarJSON_sinValidar(bloque);

                            long tiempo = System.currentTimeMillis() - inicio;

                            // LOG RAW RESPUESTA
                            log.warn("\n" +
                                            "📥 RAW RESPUESTA BLOQUE {}\n" +
                                            "(tamaño={} chars, tiempo={} ms)\n{}\n" +
                                            "─────────────────────────────────────────────",
                                    bloqueId,
                                    raw != null ? raw.length() : 0,
                                    tiempo,
                                    raw
                            );

                            // reparar
                            String reparado = repararMegaJSON(raw);

                            // LOG JSON REPARADO
                            log.warn("\n" +
                                            "🔧 JSON REPARADO BLOQUE {}\n{}\n" +
                                            "─────────────────────────────────────────────",
                                    bloqueId,
                                    reparado
                            );

                            respuestasParciales.put(bloqueId, reparado);

                            statsBloques.add(Map.of(
                                    "bloqueId", bloqueId,
                                    "chars", chars,
                                    "tiempo_ms", tiempo,
                                    "estado", "OK",
                                    "hilo", thread
                            ));

                        } catch (Exception e) {

                            long tiempo = System.currentTimeMillis() - inicio;

                            log.error("\n" +
                                            "❌ ERROR EN BLOQUE {}\n" +
                                            "Mensaje: {}\n" +
                                            "─────────────────────────────────────────────",
                                    bloqueId,
                                    e.getMessage()
                            );

                            respuestasParciales.put(bloqueId, "");

                            statsBloques.add(Map.of(
                                    "bloqueId", bloqueId,
                                    "chars", chars,
                                    "tiempo_ms", tiempo,
                                    "estado", "ERROR",
                                    "error", e.getMessage(),
                                    "hilo", thread
                            ));
                        }

                    }, iaExecutor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.warn("🟩 Todos los bloques finalizaron.");

        // ============================================================
        //  🟦 PROCESAR CADA BLOQUE Y PARSEAR JSON REPARADO
        // ============================================================
        Map<String, Object> resultadoFinal = new LinkedHashMap<>();

        for (Integer bloqueId : respuestasParciales.keySet()) {

            String json = respuestasParciales.get(bloqueId);

            if (json == null || json.isBlank()) continue;

            try {
                Map<String, Object> parsed = mapper.readValue(json, Map.class);
                resultadoFinal.putAll(parsed);

                log.warn("\n" +
                                "🟩 BLOQUE {} PARSEADO CORRECTAMENTE\n" +
                                "Keys agregadas: {}\n" +
                                "─────────────────────────────────────────────",
                        bloqueId,
                        parsed.keySet()
                );

            } catch (Exception e) {
                log.error("\n" +
                                "❌ BLOQUE {} — ERROR PARSEANDO JSON REPARADO\n" +
                                "Error: {}\n" +
                                "JSON:\n{}\n" +
                                "─────────────────────────────────────────────",
                        bloqueId,
                        e.getMessage(),
                        json
                );
            }
        }

        resultadoFinal.put("_statsBloques", statsBloques);

        return resultadoFinal;
    }


    private String repararMegaJSON(String raw) {

        if (raw == null || raw.isBlank()) return "{}";

        String txt = raw.trim();

        // Quitar texto basura ANTES o DESPUÉS del JSON
        txt = txt.replaceAll("^[^{]+", "");     // basura antes de '{'
        txt = txt.replaceAll("[^}]+$", "");     // basura después de '}'

        // Asegurar que empieza en '{'
        if (!txt.startsWith("{"))
            txt = "{" + txt;

        // Asegurar que termina en '}'
        if (!txt.endsWith("}"))
            txt = txt + "}";

        // Reparar comillas abiertas
        long count = txt.chars().filter(ch -> ch == '"').count();
        if (count % 2 != 0)
            txt += "\"";

        // Reparar comas colgantes
        txt = txt.replaceAll(",\\s*}", "}");

        // Reparar JSON con pares "key": value sin coma
        txt = txt.replaceAll("}(\\s*\"[^\"]+\":)", "},$1");

        // Reparar llaves anidadas truncadas
        txt = balancearLlaves(txt);

        return txt;
    }

    private String balancearLlaves(String s) {
        int open = 0, close = 0;

        for (char c : s.toCharArray()) {
            if (c == '{') open++;
            if (c == '}') close++;
        }

        StringBuilder sb = new StringBuilder(s);
        while (open > close) {
            sb.append("}");
            close++;
        }

        return sb.toString();
    }


    // ------------------------------------------------------
    // DIVIDIR EN BLOQUES
    // ------------------------------------------------------
    private List<Map<String, Object>> dividirEnBloques(Map<String, Object> estructura, int maxChars) throws Exception {

        // ============================================
        // 🔵 PARÁMETROS RECOMENDADOS
        // ============================================
        int LIMITE_BLOQUE = Math.min(maxChars, 8000);   // límite real para DeepSeek
        int LIMITE_ITEM = 5000;                         // si un item es demasiado grande, lo divido

        List<Object> items = new ArrayList<>();

        // Añadir párrafos
        items.addAll((List<?>) estructura.get("parrafos"));

        // Añadir tablas, pero cada fila como un item
        List<Map<String, Object>> tablas = (List<Map<String, Object>>) estructura.get("tablas");
        for (Map<String, Object> tabla : tablas) {

            List<Map<String, Object>> filas = (List<Map<String, Object>>) tabla.get("filas");
            int tablaId = (int) tabla.get("tabla");

            for (Map<String, Object> fila : filas) {

                // Clonamos la estructura de la fila, manteniendo la referencia a su tabla
                Map<String, Object> itemFila = new LinkedHashMap<>();
                itemFila.put("tabla", tablaId);
                itemFila.put("fila", fila.get("fila"));
                itemFila.put("celdas", fila.get("celdas"));

                items.add(itemFila);
            }
        }


        // ============================================
        // 🔵 ARMAR BLOQUES
        // ============================================
        List<Map<String, Object>> bloques = new ArrayList<>();

        List<Object> bloqueActual = new ArrayList<>();
        int sizeActual = 0;

        for (Object item : items) {

            String json = mapper.writeValueAsString(item);
            int sizeItem = json.length();

            // -----------------------------------
            // 🔴 ITEM DEMASIADO GRANDE → DIVIDIR
            // -----------------------------------
            if (sizeItem > LIMITE_ITEM) {
                // Dividir celdas de la fila en bloques pequeños
                bloques.addAll(dividirItemGrande(item, LIMITE_BLOQUE));
                continue;
            }

            // -----------------------------------
            // 🟡 ¿Cabe en el bloque actual?
            // -----------------------------------
            if (sizeActual + sizeItem > LIMITE_BLOQUE) {

                // cerrar bloque
                bloques.add(Map.of("items", new ArrayList<>(bloqueActual)));

                bloqueActual.clear();
                sizeActual = 0;
            }

            // agregar item seguro
            bloqueActual.add(item);
            sizeActual += sizeItem;
        }

        // agregar último bloque
        if (!bloqueActual.isEmpty()) {
            bloques.add(Map.of("items", bloqueActual));
        }

        return bloques;
    }


    /**
     * Divide un item gigante (normalmente una fila con muchas celdas o texto largo)
     * en múltiples sub-items seguros para DeepSeek.
     */
    private List<Map<String, Object>> dividirItemGrande(Object item, int limitePorBloque) throws Exception {

        List<Map<String, Object>> resultado = new ArrayList<>();

        Map<String, Object> fila = (Map<String, Object>) item;

        List<Map<String, Object>> celdas = (List<Map<String, Object>>) fila.get("celdas");

        List<Object> bloqueActual = new ArrayList<>();
        int sizeActual = 0;

        for (Map<String, Object> celda : celdas) {

            String json = mapper.writeValueAsString(celda);
            int size = json.length();

            if (sizeActual + size > limitePorBloque) {
                resultado.add(Map.of("items", new ArrayList<>(bloqueActual)));
                bloqueActual.clear();
                sizeActual = 0;
            }

            bloqueActual.add(Map.of(
                    "tabla", fila.get("tabla"),
                    "fila", fila.get("fila"),
                    "celda", celda
            ));

            sizeActual += size;
        }

        if (!bloqueActual.isEmpty()) {
            resultado.add(Map.of("items", bloqueActual));
        }

        return resultado;
    }

    // ------------------------------------------------------
    // CREAR CAMPOS DESDE IA
    // ------------------------------------------------------
    private void crearCamposDesdeIA(Map<String, Object> interpretacion, Plantilla plantilla) {

        PlantillaSeccion seccion = PlantillaSeccion.builder()
                .nombre("SECCION_GENERAL")
                .plantilla(plantilla)
                .build();

        seccionRepo.save(seccion);

        for (String nombre : interpretacion.keySet()) {

            // ❗ IGNORAR KEYS INTERNAS QUE NO SON CAMPOS
            if (nombre.startsWith("_")) continue; // _statsBloques, etc

            Object raw = interpretacion.get(nombre);

            // ❗ El valor debe ser un MAP, si no lo ignoramos
            if (!(raw instanceof Map)) {

                // Convertir cualquier cosa a MAP normalizado
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("textoOriginal", raw != null ? raw.toString() : null);
                info.put("parrafo", null);
                info.put("tabla", null);
                info.put("fila", null);
                info.put("columna", null);

                raw = info;
            }

            Map<String, Object> info = (Map<String, Object>) raw;

            PlantillaCampo campo = PlantillaCampo.builder()
                    .seccion(seccion)
                    .nombreCampo(nombre)
                    .textoOriginal((String) info.get("textoOriginal"))
                    .indexParrafo((Integer) info.get("parrafo"))
                    .indexTabla((Integer) info.get("tabla"))
                    .indexFila((Integer) info.get("fila"))
                    .indexCelda((Integer) info.get("columna"))
                    .build();

            campoRepo.save(campo);
        }
    }


    // ------------------------------------------------------
    // LIMPIAR CAMPOS ANTERIORES
    // ------------------------------------------------------
    private void limpiarEstructuraAnterior(Plantilla plantilla) {

        List<PlantillaSeccion> secs = seccionRepo.findByPlantillaId(plantilla.getId());

        for (PlantillaSeccion s : secs) {
            campoRepo.deleteBySeccionId(s.getId());
        }

        seccionRepo.deleteAll(secs);
    }
}
