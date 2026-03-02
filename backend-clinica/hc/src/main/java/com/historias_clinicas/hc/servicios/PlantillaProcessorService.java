package com.historias_clinicas.hc.servicios;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.entidades.*;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import com.historias_clinicas.hc.ia.VisionPrompts;
import com.historias_clinicas.hc.ia.gpt.GptVisionClient;
import com.historias_clinicas.hc.repositorios.PlantillaAccionRepo;
import com.historias_clinicas.hc.repositorios.PlantillaRepository;
import com.historias_clinicas.hc.repositorios.PlantillaSeccionRepository;
import com.historias_clinicas.hc.repositorios.PlantillaVisionRepo;
import com.historias_clinicas.hc.servicios.word.PdfToPngService;
import com.historias_clinicas.hc.servicios.word.WordToPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaProcessorService {

    private final ExecutorService iaExecutor;

    private final PlantillaRepository plantillaRepo;
    private final PlantillaSeccionRepository seccionRepo;
    private final DeepSeekClient deepSeekClient;
    private final PlantillaAccionRepo plantillaAccionRepo;
    private final ExecutorService deepSeekExecutor;

    private final ObjectMapper mapper = new ObjectMapper();

    // ============================================================
    // SUBIR PLANTILLA
    // ============================================================
    public Plantilla procesarPlantilla(byte[] file, String nombre) {

        log.info("📥 Registrando nueva plantilla: {}", nombre);

        Plantilla plantilla = Plantilla.builder()
                .nombre(nombre)
                .archivoOriginal(file)
                .build();

        plantillaRepo.save(plantilla);
        return plantilla;
    }

    private Map<String, Object> cargarAnalisisDesdeBD(Plantilla plantilla) {

        List<PlantillaAccion> acciones =
                plantillaAccionRepo.findByPlantillaId(plantilla.getId());

        Map<String, Object> accionesMap = new LinkedHashMap<>();

        int i = 0;
        for (PlantillaAccion a : acciones) {

            accionesMap.put("accion_" + (++i), Map.of(
                    "tabla", a.getIndexTabla(),
                    "fila", a.getIndexFila(),
                    "columna", a.getIndexColumna(),
                    "textoOriginal", a.getTextoOriginal(),
                    "accion", a.getTipoAccion().name(),
                    "descripcion", a.getDescripcion()
            ));
        }

        return Map.of(
                "origen", "BD",
                "acciones", accionesMap
        );
    }

    private void guardarAccionesComoPlantilla(
            List<Map<String, Object>> acciones,
            Plantilla plantilla,
            String hashEstructura
    ) {

        for (Map<String, Object> a : acciones) {

            String accionRaw = (String) a.get("accion");

            if (accionRaw == null) {
                log.warn("⚠️ Acción sin tipo detectada, se ignora: {}", a);
                continue;
            }


            Integer columna = (Integer) a.get("columna");

            PlantillaAccion accion = PlantillaAccion.builder()
                    .plantilla(plantilla)
                    .indexTabla((Integer) a.get("tabla"))
                    .indexFila((Integer) a.get("fila"))
                    .indexColumna(columna)
                    .textoOriginal((String) a.get("textoOriginal"))
                    .descripcion((String) a.get("descripcion"))
                    .tipoAccion(
                            TipoAccion.valueOf((String) a.get("accion"))
                    )
                    .hashEstructura(hashEstructura)
                    .build();

            plantillaAccionRepo.save(accion);
        }

        log.info("💾 {} acciones guardadas para la plantilla {}",
                acciones.size(), plantilla.getId());
    }


    //ANALISIS SIN VISION
    @Transactional
    public Map<String, Object> analizarPlantilla(Long plantillaId) throws Exception {

        long inicio = System.currentTimeMillis();
        log.info("🧠 Iniciando análisis de plantilla {}", plantillaId);

        Plantilla plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        String hashEstructura = DigestUtils.sha256Hex(
                plantilla.getArchivoOriginal()
        );

        // ♻️ Reutilización
        if (plantillaAccionRepo.existsByPlantillaId(plantillaId)) {
            log.info("♻️ Plantilla {} ya analizada, reutilizando análisis", plantillaId);
            return cargarAnalisisDesdeBD(plantilla);
        }

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal())
        );

        limpiarEstructuraAnterior(plantilla);

        // ============================================================
        // 1️⃣ POI – ESTRUCTURA FÍSICA
        // ============================================================
        log.info("📄 Extrayendo estructura POI…");

        Map<String, Object> estructuraPOI = extraerEstructuraCompacta(doc);

        List<Map<String, Object>> tablas =
                (List<Map<String, Object>>) estructuraPOI.getOrDefault("tablas", List.of());

        List<Map<String, Object>> parrafos =
                (List<Map<String, Object>>) estructuraPOI.getOrDefault("parrafos", List.of());

        List<Map<String, Object>> accionesTotales = new ArrayList<>();
        log.info("📐 POI listo: {} tablas, {} párrafos", tablas.size(), parrafos.size());

        // ============================================================
        // 2️⃣ DEEPSEEK EN PARALELO (TABLAS)
        // ============================================================
        log.info("⚡ Extrayendo campos editables con DeepSeek (solo POI)…");

        List<Future<List<Map<String, Object>>>> futures = new ArrayList<>();

        int FILAS_POR_BLOQUE = 8; // 👈 ajustable

        for (Map<String, Object> tabla : tablas) {

            List<Map<String, Object>> bloques =
                    dividirTablaEnBloques(tabla, FILAS_POR_BLOQUE);

            for (Map<String, Object> tablaBloque : bloques) {
                // 👇 PROCESAR GEOMÉTRICAMENTE
                procesarBloqueDetectado(
                        tabla,
                        (List<Map<String, Object>>) tablaBloque.get("filas"),
                        (Integer) tablaBloque.get("anchoTotalTabla"),
                        accionesTotales
                );

                futures.add(
                        deepSeekExecutor.submit(() -> {

                            Map<String, Object> poiParcial = Map.of(
                                    "tablas", List.of(tablaBloque)
                            );

                            return deepSeekClient.conciliarRegion(
                                    poiParcial,
                                    null   // 👈 SIN VISIÓN
                            );
                        })
                );
            }
        }

        // ============================================================
        // 2️⃣B DEEPSEEK PARA PÁRRAFOS (SIN VISIÓN)
        // ============================================================
        if (!parrafos.isEmpty()) {

            log.info("🧠 Procesando párrafos como unidades editables…");

            futures.add(
                    deepSeekExecutor.submit(() -> {

                        List<Map<String, Object>> filas = new ArrayList<>();

                        for (Map<String, Object> p : parrafos) {

                            Map<String, Object> celda = new LinkedHashMap<>();
                            celda.put("tabla", null);
                            celda.put("fila", p.get("indexParrafo"));
                            celda.put("columna", null);
                            celda.put("texto", p.get("texto"));

                            Map<String, Object> fila = new LinkedHashMap<>();
                            fila.put("fila", p.get("indexParrafo"));
                            fila.put("celdas", List.of(celda));

                            filas.add(fila);
                        }

                        Map<String, Object> tablaParrafos = new LinkedHashMap<>();
                        tablaParrafos.put("tabla", null);
                        tablaParrafos.put("filas", filas);

                        Map<String, Object> poiParrafos = new LinkedHashMap<>();
                        poiParrafos.put("tablas", List.of(tablaParrafos));

                        return deepSeekClient.conciliarRegion(
                                poiParrafos,
                                null   // 👈 SIN VISIÓN
                        );
                    })
            );
        }

        // ============================================================
        // 3️⃣ RECOLECCIÓN DE RESULTADOS
        // ============================================================
        for (Future<List<Map<String, Object>>> future : futures) {
            try {
                List<Map<String, Object>> resultado = future.get();
                if (resultado != null && !resultado.isEmpty()) {
                    accionesTotales.addAll(resultado);
                }
            } catch (Exception e) {
                Throwable root = (e instanceof java.util.concurrent.ExecutionException ex && ex.getCause() != null)
                        ? ex.getCause()
                        : e;

                log.error("💥 Error en tarea DeepSeek: {}", root.getMessage(), root);
            }
        }

        log.info("🧾 Total de campos detectados: {}", accionesTotales.size());

        // ============================================================
        // 4️⃣ GUARDADO
        // ============================================================
        guardarAccionesComoPlantilla(
                accionesTotales,
                plantilla,
                hashEstructura
        );

        return Map.of(
                "origen", "POI+DEEPSEEK+THREADS",
                "plantillaId", plantillaId,
                "duracion_ms", System.currentTimeMillis() - inicio,
                "accionesDetectadas", accionesTotales.size()
        );
    }

    // ============================================================
    // EXTRACTOR POI (ESTRUCTURA CANÓNICA)
    // ============================================================
    private Map<String, Object> extraerEstructuraCompacta(XWPFDocument doc) {

        Map<String, Object> data = new LinkedHashMap<>();

        // ==========================
        // PÁRRAFOS
        // ==========================
        List<Map<String, Object>> parrafos = new ArrayList<>();
        int p = 0;

        for (XWPFParagraph parrafo : doc.getParagraphs()) {

            String txt = Optional.ofNullable(parrafo.getText())
                    .orElse("")
                    .trim();

            if (!txt.isEmpty()) {
                parrafos.add(Map.of(
                        "tipo", "parrafo",
                        "indexParrafo", p,
                        "texto", txt
                ));
            }

            p++;
        }

        data.put("parrafos", parrafos);

        // ==========================
        // TABLAS
        // ==========================
        List<Map<String, Object>> tablas = new ArrayList<>();
        int t = 0;

        for (XWPFTable tabla : doc.getTables()) {

            List<Map<String, Object>> filas = new ArrayList<>();
            int anchoMaximoTabla = 0;

            // Mapa para seguimiento de merges verticales activos
            Map<Integer, Integer> verticalMergesActivos = new HashMap<>();

            int f = 0;

            for (XWPFTableRow row : tabla.getRows()) {

                List<Map<String, Object>> celdas = new ArrayList<>();
                int columnaVisual = 0;

                for (XWPFTableCell celda : row.getTableCells()) {

                    String texto = Optional.ofNullable(celda.getText())
                            .orElse("")
                            .replace("\n", " ")
                            .trim();

                    int spanHorizontal = 1;
                    int spanVertical = 1;
                    boolean esContinuacionVertical = false;

                    var tcPr = celda.getCTTc().getTcPr();

                    // ===== GRIDSPAN (horizontal)
                    if (tcPr != null && tcPr.getGridSpan() != null) {
                        spanHorizontal = tcPr.getGridSpan()
                                .getVal()
                                .intValue();
                    }

                    // ===== VMERGE (vertical)
                    if (tcPr != null && tcPr.getVMerge() != null) {

                        STMerge.Enum vMergeVal = tcPr.getVMerge().getVal();

                        if (vMergeVal == null || vMergeVal == STMerge.CONTINUE) {

                            esContinuacionVertical = true;
                            spanVertical = verticalMergesActivos
                                    .getOrDefault(columnaVisual, 1);

                        } else if (vMergeVal == STMerge.RESTART) {

                            spanVertical = 1;
                            verticalMergesActivos.put(columnaVisual, 1);
                        }
                    }

                    // Si es restart vertical, marcar como activo
                    if (!esContinuacionVertical && tcPr != null && tcPr.getVMerge() != null) {
                        verticalMergesActivos.put(columnaVisual, spanVertical);
                    }

                    Map<String, Object> celdaMap = new LinkedHashMap<>();
                    celdaMap.put("tabla", t);
                    celdaMap.put("fila", f);
                    celdaMap.put("columnaVisual", columnaVisual);
                    celdaMap.put("texto", texto);
                    celdaMap.put("spanHorizontal", spanHorizontal);
                    celdaMap.put("spanVertical", spanVertical);
                    celdaMap.put("esContinuacionVertical", esContinuacionVertical);
                    celdaMap.put("esVacia", texto.isBlank());

                    celdas.add(celdaMap);

                    columnaVisual += spanHorizontal;
                }

                anchoMaximoTabla = Math.max(anchoMaximoTabla, columnaVisual);

                Map<String, Object> filaMap = new LinkedHashMap<>();
                filaMap.put("fila", f);
                filaMap.put("totalColumnasVisuales", columnaVisual);
                filaMap.put("celdas", celdas);

                filas.add(filaMap);
                f++;
            }

            calcularAlturaReal(filas);

            // ==========================
            // NORMALIZACIÓN DE ANCHO
            // ==========================
            for (Map<String, Object> fila : filas) {
                fila.put("anchoTotalTabla", anchoMaximoTabla);
            }

            Map<String, Object> tablaMap = new LinkedHashMap<>();
            tablaMap.put("tabla", t);
            tablaMap.put("anchoTotalTabla", anchoMaximoTabla);
            tablaMap.put("filas", filas);

            tablas.add(tablaMap);
            t++;
        }

        data.put("tablas", tablas);

        return data;
    }

    private void calcularAlturaReal(List<Map<String, Object>> filas) {

        for (int i = 0; i < filas.size(); i++) {

            Map<String, Object> fila = filas.get(i);
            List<Map<String, Object>> celdas =
                    (List<Map<String, Object>>) fila.get("celdas");

            for (Map<String, Object> celda : celdas) {

                boolean esContinuacion =
                        Boolean.TRUE.equals(celda.get("esContinuacionVertical"));

                if (esContinuacion) continue;

                Integer columna = (Integer) celda.get("columnaVisual");
                if (columna == null) continue;
                int altura = 1;

                // mirar hacia abajo
                for (int j = i + 1; j < filas.size(); j++) {

                    Map<String, Object> filaInferior = filas.get(j);
                    List<Map<String, Object>> celdasInferior =
                            (List<Map<String, Object>>) filaInferior.get("celdas");

                    Optional<Map<String, Object>> match =
                            celdasInferior.stream()
                                    .filter(c ->
                                            (int) c.get("columnaVisual") == columna
                                                    && Boolean.TRUE.equals(c.get("esContinuacionVertical"))
                                    )
                                    .findFirst();

                    if (match.isPresent()) {
                        altura++;
                    } else {
                        break;
                    }
                }

                celda.put("spanVerticalReal", altura);
            }
        }
    }

    private List<Map<String, Object>> dividirTablaEnBloques(
            Map<String, Object> tabla,
            int filasPorBloque
    ) {

        List<Map<String, Object>> bloques = new ArrayList<>();

        Integer indexTabla = (Integer) tabla.get("tabla");
        List<Map<String, Object>> filas =
                (List<Map<String, Object>>) tabla.get("filas");

        for (int i = 0; i < filas.size(); i += filasPorBloque) {

            int fin = Math.min(i + filasPorBloque, filas.size());

            List<Map<String, Object>> subFilas =
                    filas.subList(i, fin);

            Map<String, Object> tablaParcial = new LinkedHashMap<>();
            tablaParcial.put("tabla", indexTabla);
            tablaParcial.put("filas", subFilas);
            tablaParcial.put("anchoTotalTabla", tabla.get("anchoTotalTabla"));

            bloques.add(tablaParcial);
        }

        return bloques;
    }

    // ============================================================
    // JSON FIXER
    // ============================================================
    private String repararMegaJSON(String raw) {

        if (raw == null || raw.isBlank()) return "{}";

        String txt = raw.trim()
                .replaceAll("^[^{]+", "")
                .replaceAll("[^}]+$", "");

        if (!txt.startsWith("{")) txt = "{" + txt;
        if (!txt.endsWith("}")) txt += "}";

        return txt;
    }

    // ============================================================
// SOLO POI – ESTRUCTURA CRUDA (DEBUG / INSPECCIÓN)
// ============================================================
    @Transactional(readOnly = true)
    public Map<String, Object> extraerEstructuraPoi(Long plantillaId) throws Exception {

        log.info("🔍 Extrayendo estructura POI para plantilla {}", plantillaId);

        Plantilla plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal())
        );

        Map<String, Object> estructura = extraerEstructuraCompacta(doc);

        return Map.of(
                "origen", "POI",
                "plantillaId", plantillaId,
                "estructura", estructura
        );
    }

    public List<Map<String, Object>> procesarChecklists(
            Long plantillaId,
            String textoClinico
    ) throws Exception {

        Plantilla plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal())
        );

        Map<String, Object> estructura = extraerEstructuraCompacta(doc);

        List<Map<String, Object>> tablas =
                (List<Map<String, Object>>) estructura.getOrDefault("tablas", List.of());

        List<Map<String, Object>> accionesChecklist = new ArrayList<>();

        for (Map<String, Object> tabla : tablas) {

            List<Map<String, Object>> bloques =
                    detectarTablasRealesEnTabla(tabla);

            for (Map<String, Object> bloque : bloques) {

                if (!"CHECKLIST".equals(bloque.get("tipo"))) continue;

                // 🔒 Extraer mapa interno
                Map<String, Object> modeloIA =
                        new LinkedHashMap<>(bloque);

                Map<String, Map<String, Object>> mapaInterno =
                        (Map<String, Map<String, Object>>) modeloIA.remove("_mapaCoordenadas");

                Map<String, Object> payload = Map.of(
                        "texto_clinico", textoClinico,
                        "checklist", modeloIA
                );

                String raw = deepSeekClient.completarJSON_sinValidar(
                        payload,
                        DeepSeekClient.IA_CHECKLIST_PROMPT
                );

                String limpio = raw.trim()
                        .replaceAll("```json", "")
                        .replaceAll("```", "");

                Map<String, Object> respuesta =
                        mapper.readValue(limpio, Map.class);

                List<String> ids =
                        (List<String>) respuesta.getOrDefault("marcar", List.of());

                for (String id : ids) {

                    Map<String, Object> coords = mapaInterno.get(id);

                    if (coords != null) {
                        accionesChecklist.add(Map.of(
                                "tabla", coords.get("tabla"),
                                "fila", coords.get("fila"),
                                "columna", coords.get("columnaMarcable"),
                                "accion", "MARCAR"
                        ));
                    }
                }
            }
        }

        return accionesChecklist;
    }

    // ============================================================
    // LIMPIEZA
    // ============================================================
    private void limpiarEstructuraAnterior(Plantilla plantilla) {

        seccionRepo.deleteByPlantillaId(plantilla.getId());
        plantillaAccionRepo.deleteByPlantillaId(plantilla.getId());

        log.info("🧹 Estructura anterior eliminada (secciones + acciones)");
    }


    public List<Map<String, Object>> detectarTablasRealesEnTabla(
            Map<String, Object> tabla
    ) {


        if (tabla == null) return List.of();

        Object filasObj = tabla.get("filas");

        if (!(filasObj instanceof List<?> filasRaw)) {
            return List.of(); // no es tabla estructural cruda
        }

        List<Map<String, Object>> filas =
                (List<Map<String, Object>>) filasRaw;

        if (filas.isEmpty()) return List.of();

        int anchoTotalTabla = Optional
                .ofNullable((Integer) tabla.get("anchoTotalTabla"))
                .orElse(0);

        List<Map<String, Object>> resultado = new ArrayList<>();

        List<Map<String, Object>> bloqueActual = new ArrayList<>();
        String firmaBase = null;

        for (int i = 0; i < filas.size(); i++) {

            Map<String, Object> fila = filas.get(i);
            List<Map<String, Object>> celdas =
                    (List<Map<String, Object>>) fila.get("celdas");

            String firma = firmaGeomFila(celdas);

            // 🔹 Cortar por título
            if (esFilaTitulo(celdas, anchoTotalTabla)) {

                procesarBloqueDetectado(
                        tabla,
                        bloqueActual,
                        anchoTotalTabla,
                        resultado
                );

                bloqueActual = new ArrayList<>();
                firmaBase = null;
                continue;
            }

            if (firmaBase == null) {
                firmaBase = firma;
                bloqueActual.add(fila);
                continue;
            }

            if (firma.equals(firmaBase)
                    || distanciaFirma(firmaBase, firma) <= 1) {

                bloqueActual.add(fila);

            } else {

                procesarBloqueDetectado(
                        tabla,
                        bloqueActual,
                        anchoTotalTabla,
                        resultado
                );

                bloqueActual = new ArrayList<>();
                bloqueActual.add(fila);
                firmaBase = firma;
            }
        }

        // último bloque
        procesarBloqueDetectado(
                tabla,
                bloqueActual,
                anchoTotalTabla,
                resultado
        );

        return resultado;
    }


    private void procesarBloqueDetectado(
            Map<String, Object> tablaOriginal,
            List<Map<String, Object>> bloque,
            int anchoTotalTabla,
            List<Map<String, Object>> acumulador
    ) {

        if (!esBloqueTablaGeometrico(bloque, anchoTotalTabla)) return;

        String tipo = clasificarBloque(bloque, anchoTotalTabla);

        if ("CHECKLIST".equals(tipo)) {

            Map<String, Object> bloqueChecklist = new LinkedHashMap<>();
            bloqueChecklist.put("tabla", tablaOriginal.get("tabla"));
            bloqueChecklist.put("filas", bloque);

            Map<String, Object> generico =
                    construirModeloChecklistGenerico(
                            bloqueChecklist,
                            anchoTotalTabla
                    );

            Map<String, Object> bloqueProcesado =
                    construirChecklistAgrupadoGenerico(generico);

            acumulador.add(bloqueProcesado);

        }
        else if ("TABLA_MATRIZ".equals(tipo)) {

            Map<String, Object> parcial = crearTablaParcial(
                    tablaOriginal,
                    bloque
            );

            acumulador.add(parcial);
        }
    }

    private void generarCamposTablaAutonoma(
            Map<String, Object> tablaOriginal,
            List<Map<String, Object>> bloque,
            List<Map<String, Object>> acumulador
    ) {

        if (bloque == null || bloque.size() < 2) return;

        Integer indexTabla = (Integer) tablaOriginal.get("tabla");

        // 1️⃣ Fila encabezado
        Map<String, Object> filaHeader = bloque.get(0);
        List<Map<String, Object>> celdasHeader =
                (List<Map<String, Object>>) filaHeader.get("celdas");

        // 2️⃣ Recorrer filas de datos
        for (int i = 1; i < bloque.size(); i++) {

            Map<String, Object> filaDatos = bloque.get(i);
            List<Map<String, Object>> celdasDatos =
                    (List<Map<String, Object>>) filaDatos.get("celdas");

            // Etiqueta estructural de fila (primera celda no vacía)
            String etiquetaFila = obtenerEtiquetaFila(celdasDatos);

            for (Map<String, Object> celda : celdasDatos) {

                boolean esVacia =
                        Boolean.TRUE.equals(celda.get("esVacia"));

                boolean esContinuacion =
                        Boolean.TRUE.equals(celda.get("esContinuacionVertical"));

                if (!esVacia || esContinuacion) continue;

                Integer columnaVisual = (Integer) celda.get("columnaVisual");
                if (columnaVisual == null) {
                    log.warn("Celda sin columnaVisual: {}", celda);
                    continue;
                }

                Map<String, Object> headerMatch =
                        buscarHeaderPorColumna(celdasHeader, columnaVisual);

                if (headerMatch == null) continue;

                String headerTexto =
                        String.valueOf(headerMatch.get("texto")).trim();

                if (headerTexto.isBlank()) continue;

                String descripcion =
                        construirDescripcion(headerTexto, etiquetaFila);

                acumulador.add(Map.of(
                        "tabla", indexTabla,
                        "fila", filaDatos.get("fila"),
                        "columna", columnaVisual,
                        "textoOriginal", "",
                        "accion", "EDITAR_CAMPO",
                        "descripcion", descripcion
                ));
            }
        }
    }

    private String obtenerEtiquetaFila(
            List<Map<String, Object>> celdas
    ) {

        for (Map<String, Object> c : celdas) {

            boolean esVacia =
                    Boolean.TRUE.equals(c.get("esVacia"));

            if (!esVacia) {
                return String.valueOf(c.get("texto")).trim();
            }
        }

        return "";
    }

    private String construirDescripcion(
            String header,
            String etiquetaFila
    ) {

        if (etiquetaFila == null || etiquetaFila.isBlank()) {
            return "Valor de la columna " + header;
        }

        return "Valor de la columna " + header +
                " para la fila " + etiquetaFila;
    }

    private Map<String, Object> buscarHeaderPorColumna(
            List<Map<String, Object>> headers,
            int columna
    ) {

        for (Map<String, Object> h : headers) {

            Integer col = (Integer) h.get("columnaVisual");
            Integer spanH = (Integer) h.get("spanHorizontal");

            if (col == null || spanH == null) continue;

            if (columna >= col && columna < col + spanH) {
                return h;
            }
        }

        return null;
    }
    /**
     * Firma geométrica por fila:
     * Ordena por columnaVisual y concatena: col:spanH:spanVReal:contV
     * No usa texto.
     */
    private String firmaGeomFila(List<Map<String, Object>> celdas) {

        List<Map<String, Object>> ordenadas = new ArrayList<>(celdas);
        ordenadas.sort(Comparator.comparingInt(c -> (int) c.getOrDefault("columnaVisual", 0)));

        StringBuilder sb = new StringBuilder();

        for (Map<String, Object> c : ordenadas) {

            int col = (int) c.getOrDefault("columnaVisual", 0);
            int spanH = (int) c.getOrDefault("spanHorizontal", 1);

            // preferir spanVerticalReal si existe; si no, caer a spanVertical
            Object svr = c.get("spanVerticalReal");
            int spanVReal = (svr instanceof Integer) ? (Integer) svr : (int) c.getOrDefault("spanVertical", 1);

            boolean contV = Boolean.TRUE.equals(c.get("esContinuacionVertical"));

            sb.append(col).append(":")
                    .append(spanH).append(":")
                    .append(spanVReal).append(":")
                    .append(contV ? "1" : "0")
                    .append("|");
        }

        return sb.toString();
    }

    /**
     * Detecta filas tipo título:
     * - una celda con spanHorizontal == anchoTotalTabla
     * - o la suma de spanHorizontal de una sola celda domina todo el ancho
     */
    private boolean esFilaTitulo(List<Map<String, Object>> celdas, int anchoTotalTabla) {
        if (anchoTotalTabla <= 0 || celdas == null || celdas.isEmpty()) return false;

        if (celdas.size() == 1) {
            int spanH = (int) celdas.get(0).getOrDefault("spanHorizontal", 1);
            return spanH >= anchoTotalTabla;
        }

        // Si existe alguna celda que ocupe todo el ancho, es título
        for (Map<String, Object> c : celdas) {
            int spanH = (int) c.getOrDefault("spanHorizontal", 1);
            if (spanH >= anchoTotalTabla) return true;
        }

        return false;
    }

    /**
     * Distancia simple entre firmas:
     * cuenta cuántos "tokens de celda" difieren.
     * (tolerancia mínima para que no se rompa por detalles menores)
     */
    private int distanciaFirma(String a, String b) {
        String[] ta = a.split("\\|");
        String[] tb = b.split("\\|");

        int len = Math.min(ta.length, tb.length);
        int dist = 0;

        for (int i = 0; i < len; i++) {
            if (!ta[i].equals(tb[i])) dist++;
        }

        dist += Math.abs(ta.length - tb.length);
        return dist;
    }

    /**
     * Decide si un bloque es subtabla real (geométrica):
     * - mínimo 3 filas
     * - ancho consistente (múltiples columnas visuales)
     * - no dominado por merges verticales gigantes (formulario)
     */
    private boolean esBloqueTablaGeometrico(List<Map<String, Object>> bloque, int anchoTotalTabla) {
        if (bloque == null || bloque.size() < 3) return false;

        // Evaluar la primera fila como "estructura base"
        List<Map<String, Object>> celdas0 =
                (List<Map<String, Object>>) bloque.get(0).get("celdas");

        if (celdas0 == null || celdas0.isEmpty()) return false;

        // 1) Debe haber múltiples columnas visuales (más de 1 celda efectiva o spans que dividan)
        int columnasEfectivas = contarColumnasEfectivas(celdas0);
        if (columnasEfectivas <= 1) return false;

        // 2) Evitar formularios: si hay merges verticales grandes dominantes en la mayoría de filas
        if (esDominadoPorMergesVerticales(bloque)) return false;

        if (esLayoutDominadoPorColumnaGigante(bloque, anchoTotalTabla)) return false;

        // 3) Evitar títulos repetidos
        if (esFilaTitulo(celdas0, anchoTotalTabla)) return false;

        return true;
    }

    private boolean esLayoutDominadoPorColumnaGigante(List<Map<String, Object>> bloque, int anchoTotalTabla) {

        if (bloque == null || bloque.isEmpty()) return false;

        List<Map<String, Object>> celdas0 =
                (List<Map<String, Object>>) bloque.get(0).get("celdas");

        for (Map<String, Object> c : celdas0) {

            int spanH = (int) c.getOrDefault("spanHorizontal", 1);

            double proporcion = (double) spanH / (double) anchoTotalTabla;

            if (proporcion >= 0.8) {
                return true; // layout tipo formulario ancho completo
            }
        }

        return false;
    }

    private int contarColumnasEfectivas(List<Map<String, Object>> celdas) {
        // Contar “puntos de inicio” de columnas: diferente columnaVisual
        Set<Integer> cols = new HashSet<>();
        for (Map<String, Object> c : celdas) {
            cols.add((int) c.getOrDefault("columnaVisual", 0));
        }
        return cols.size();
    }

    /**
     * Heurística estructural (no textual) para distinguir formulario vs matriz:
     * Si muchas filas tienen celdas con spanVerticalReal grande (>=3), suele ser formulario.
     */
    private boolean esDominadoPorMergesVerticales(List<Map<String, Object>> bloque) {
        int filasConMergeGrande = 0;

        for (Map<String, Object> fila : bloque) {
            List<Map<String, Object>> celdas =
                    (List<Map<String, Object>>) fila.get("celdas");

            if (celdas == null) continue;

            boolean hayGrande = false;

            for (Map<String, Object> c : celdas) {
                Object svr = c.get("spanVerticalReal");
                int spanVReal = (svr instanceof Integer) ? (Integer) svr : (int) c.getOrDefault("spanVertical", 1);

                boolean contV = Boolean.TRUE.equals(c.get("esContinuacionVertical"));
                if (!contV && spanVReal >= 3) {
                    hayGrande = true;
                    break;
                }
            }

            if (hayGrande) filasConMergeGrande++;
        }

        // Si más del 60% de filas tiene merges verticales grandes, probablemente es formulario
        return filasConMergeGrande >= Math.ceil(bloque.size() * 0.6);
    }

    private int[] construirVectorEstructural(List<Map<String, Object>> celdas) {

        int[] vector = new int[celdas.size()];

        for (int i = 0; i < celdas.size(); i++) {
            String texto = (String) celdas.get(i).get("texto");
            vector[i] = (texto != null && !texto.isBlank()) ? 1 : 0;
        }

        return vector;
    }

    private int distanciaHamming(int[] a, int[] b) {

        int len = Math.min(a.length, b.length);
        int dist = 0;

        for (int i = 0; i < len; i++) {
            if (a[i] != b[i]) dist++;
        }

        return dist;
    }

    private boolean esTablaRepetitiva(List<Map<String, Object>> bloque) {

        if (bloque.size() < 3) return false;

        // verificar que tenga más de una columna activa
        List<Map<String, Object>> celdas =
                (List<Map<String, Object>>) bloque.get(0).get("celdas");

        long columnasActivas = celdas.stream()
                .filter(c -> {
                    String t = (String) c.get("texto");
                    return t != null && !t.isBlank();
                })
                .count();

        return columnasActivas > 1;
    }

    private Map<String, Object> crearTablaParcial(
            Map<String, Object> tablaOriginal,
            List<Map<String, Object>> filas
    ) {

        int anchoTotalTabla =
                (int) tablaOriginal.getOrDefault("anchoTotalTabla", 0);

        String tipo = clasificarBloque(filas, anchoTotalTabla);

        Map<String, Object> parcial = new LinkedHashMap<>();
        parcial.put("tabla", tablaOriginal.get("tabla"));
        parcial.put("tipo", tipo);
        parcial.put("filas", new ArrayList<>(filas));

        return parcial;
    }

    private String clasificarBloque(
            List<Map<String, Object>> bloque,
            int anchoTotalTabla
    ) {

        if (esChecklist(bloque, anchoTotalTabla)) {
            return "CHECKLIST";
        }

        if (esTablaMatrizEditable(bloque, anchoTotalTabla)) {
            return "TABLA_MATRIZ";
        }

        return "OTRO";
    }

    private boolean esChecklist(List<Map<String, Object>> bloque, int anchoTotalTabla) {

        if (bloque == null || bloque.size() < 1) return false;
        if (anchoTotalTabla <= 0) return false;

        int filasConPatron = 0;

        for (Map<String, Object> fila : bloque) {

            List<Map<String, Object>> celdas =
                    (List<Map<String, Object>>) fila.get("celdas");

            if (celdas == null || celdas.size() < 3) continue;

            int columnasPequenas = 0;
            int columnasGrandes = 0;

            for (Map<String, Object> c : celdas) {

                int spanH = (int) c.getOrDefault("spanHorizontal", 1);
                double proporcion = (double) spanH / anchoTotalTabla;

                if (proporcion <= 0.12) {
                    columnasPequenas++;
                } else {
                    columnasGrandes++;
                }
            }

            if (columnasPequenas >= 2 && columnasGrandes >= 2) {

                if (hayAlternancia(celdas, anchoTotalTabla)) {
                    filasConPatron++;
                }
            }
        }

        return filasConPatron >= 2;
    }

    private boolean hayAlternancia(List<Map<String, Object>> celdas, int anchoTotalTabla) {

        if (celdas.size() < 3) return false;

        int cambios = 0;

        for (int i = 1; i < celdas.size(); i++) {

            int spanAnterior =
                    (int) celdas.get(i - 1).getOrDefault("spanHorizontal", 1);

            int spanActual =
                    (int) celdas.get(i).getOrDefault("spanHorizontal", 1);

            boolean anteriorPequeno =
                    ((double) spanAnterior / anchoTotalTabla) <= 0.12;

            boolean actualPequeno =
                    ((double) spanActual / anchoTotalTabla) <= 0.12;

            if (anteriorPequeno != actualPequeno) {
                cambios++;
            }
        }

        return cambios >= 2;
    }

    private List<Map<String, Object>> extraerOpcionesChecklistDesdeFila(
            Map<String, Object> fila,
            int anchoTotalTabla
    ) {

        List<Map<String, Object>> resultado = new ArrayList<>();

        List<Map<String, Object>> celdas =
                (List<Map<String, Object>>) fila.get("celdas");

        if (celdas == null) return resultado;

        for (int i = 0; i < celdas.size() - 1; i++) {

            Map<String, Object> actual = celdas.get(i);
            Map<String, Object> siguiente = celdas.get(i + 1);

            int spanActual =
                    (int) actual.getOrDefault("spanHorizontal", 1);

            int spanSiguiente =
                    (int) siguiente.getOrDefault("spanHorizontal", 1);

            double propActual = (double) spanActual / anchoTotalTabla;
            double propSiguiente = (double) spanSiguiente / anchoTotalTabla;

            String textoActual =
                    (String) actual.getOrDefault("texto", "");

            // Patrón: Texto grande seguido de celda pequeña
            if (!textoActual.isBlank()
                    && propActual > 0.12
                    && propSiguiente <= 0.12) {

                Map<String, Object> opcion = new LinkedHashMap<>();

                opcion.put("texto", textoActual);
                opcion.put("fila", fila.get("fila"));
                opcion.put("columnaMarcable",
                        siguiente.get("columnaVisual"));

                // Detectar si ya está marcado (ej: contiene "x")
                String textoMarcador =
                        (String) siguiente.getOrDefault("texto", "");

                opcion.put("marcado",
                        textoMarcador != null
                                && !textoMarcador.isBlank());

                resultado.add(opcion);
            }
        }

        return resultado;
    }

    public Map<String, Object> extraerChecklistDesdeBloque(
            Map<String, Object> bloque,
            int anchoTotalTabla
    ) {

        List<Map<String, Object>> filas =
                (List<Map<String, Object>>) bloque.get("filas");

        List<Map<String, Object>> opcionesTotales = new ArrayList<>();

        for (Map<String, Object> fila : filas) {

            opcionesTotales.addAll(
                    extraerOpcionesChecklistDesdeFila(
                            fila,
                            anchoTotalTabla
                    )
            );
        }

        Map<String, Object> resultado = new LinkedHashMap<>();

        resultado.put("tabla", bloque.get("tabla"));
        resultado.put("tipo", "CHECKLIST");
        resultado.put("opciones", opcionesTotales);

        return resultado;
    }

    private Map<String, Object> construirModeloChecklistGenerico(
            Map<String, Object> bloque,
            int anchoTotalTabla
    ) {

        List<Map<String, Object>> filas =
                (List<Map<String, Object>>) bloque.get("filas");

        List<Map<String, Object>> items = new ArrayList<>();

        if (filas == null || filas.isEmpty()) {
            return Map.of(
                    "tabla", bloque.get("tabla"),
                    "tipo", "CHECKLIST",
                    "items", items
            );
        }

        for (Map<String, Object> fila : filas) {

            Integer filaIndex = (Integer) fila.get("fila");

            List<Map<String, Object>> celdas =
                    (List<Map<String, Object>>) fila.get("celdas");

            if (celdas == null || celdas.size() < 2) continue;

            int paresEnFila = 0;
            int startIndexItems = items.size();

            for (int i = 0; i < celdas.size() - 1; i++) {

                Map<String, Object> actual = celdas.get(i);
                Map<String, Object> siguiente = celdas.get(i + 1);

                boolean actualVacio =
                        Boolean.TRUE.equals(actual.get("esVacia"));

                boolean siguienteVacio =
                        Boolean.TRUE.equals(siguiente.get("esVacia"));

                int spanActual =
                        (int) actual.getOrDefault("spanHorizontal", 1);

                int spanSiguiente =
                        (int) siguiente.getOrDefault("spanHorizontal", 1);

                // 🔥 Regla puramente relativa
                boolean celdaTexto =
                        !actualVacio && spanActual > spanSiguiente;

                boolean celdaMarcable =
                        siguienteVacio && spanSiguiente < spanActual;

                if (celdaTexto && celdaMarcable) {

                    Map<String, Object> item = new LinkedHashMap<>();

                    item.put("tabla", bloque.get("tabla"));
                    item.put("fila", filaIndex);
                    item.put("columnaMarcable",
                            siguiente.get("columnaVisual"));
                    item.put("columnaTexto",
                            actual.get("columnaVisual"));
                    item.put("textoOpcion",
                            actual.get("texto"));
                    item.put("anchoTotalTabla", anchoTotalTabla);

                    items.add(item);
                    paresEnFila++;
                }
            }

            // 🛡 Blindaje: evitar encabezados aislados
            if (paresEnFila < 2) {
                // remover los que se agregaron en esta fila
                items.subList(startIndexItems, items.size()).clear();
            }
        }

        return Map.of(
                "tabla", bloque.get("tabla"),
                "tipo", "CHECKLIST",
                "items", items,
                "filas", bloque.get("filas")
        );
    }

    private Map<String, Object> construirChecklistAgrupadoGenerico(
            Map<String, Object> modeloChecklistGenerico
    ) {

        List<Map<String, Object>> items =
                (List<Map<String, Object>>) modeloChecklistGenerico
                        .getOrDefault("items", List.of());

        List<Map<String, Object>> filasOriginales =
                (List<Map<String, Object>>) modeloChecklistGenerico
                        .getOrDefault("filas", List.of());

        Map<Integer, List<Map<String, Object>>> itemsPorFila = new LinkedHashMap<>();

        // 1️⃣ Agrupar items por fila
        for (Map<String, Object> item : items) {

            Integer fila = (Integer) item.get("fila");

            itemsPorFila
                    .computeIfAbsent(fila, k -> new ArrayList<>())
                    .add(item);
        }

        List<Map<String, Object>> grupos = new ArrayList<>();
        Map<String, Map<String, Object>> mapaInterno = new LinkedHashMap<>();

        String grupoActual = null;

        // 2️⃣ Procesar filas en orden natural
        for (Integer filaIndex : itemsPorFila.keySet()) {

            List<Map<String, Object>> itemsFila = itemsPorFila.get(filaIndex);

            // 🔎 Detectar columnas usadas por opciones
            Set<Integer> columnasOpciones = new HashSet<>();

            for (Map<String, Object> item : itemsFila) {
                columnasOpciones.add((Integer) item.get("columnaTexto"));
                columnasOpciones.add((Integer) item.get("columnaMarcable"));
            }

            // 🔎 Buscar fila original
            Map<String, Object> filaOriginal = filasOriginales.stream()
                    .filter(f -> filaIndex.equals(f.get("fila")))
                    .findFirst()
                    .orElse(null);

            String textoContexto = null;

            if (filaOriginal != null) {

                List<Map<String, Object>> celdas =
                        (List<Map<String, Object>>) filaOriginal.get("celdas");

                if (celdas != null) {

                    for (Map<String, Object> celda : celdas) {

                        Integer col = (Integer) celda.get("columnaVisual");

                        // Si no pertenece al patrón alternante → es contexto
                        if (!columnasOpciones.contains(col)) {

                            String txt = (String) celda.get("texto");

                            if (txt != null && !txt.isBlank()) {
                                textoContexto = txt;
                                break;
                            }
                        }
                    }
                }
            }

            // 🔁 Si encontramos contexto nuevo, actualizamos grupoActual
            if (textoContexto != null) {
                grupoActual = textoContexto;
            }

            if (grupoActual == null) {
                grupoActual = "SIN_GRUPO";
            }

            // 🔨 Construir lista de opciones para esta fila
            List<Map<String, Object>> opcionesFila = new ArrayList<>();

            for (Map<String, Object> item : itemsFila) {

                String texto = (String) item.get("textoOpcion");
                String id = item.get("fila") + "_" + item.get("columnaMarcable");

                mapaInterno.put(id, item);

                opcionesFila.add(Map.of(
                        "id", id,
                        "texto", texto
                ));
            }

            final String grupoActualFinal = grupoActual;

            Optional<Map<String, Object>> grupoExistente =
                    grupos.stream()
                            .filter(g -> g.get("grupo").equals(grupoActualFinal))
                            .findFirst();

            if (grupoExistente.isPresent()) {

                List<Map<String, Object>> lista =
                        (List<Map<String, Object>>) grupoExistente.get().get("opciones");

                lista.addAll(opcionesFila);

            } else {

                Map<String, Object> nuevoGrupo = new LinkedHashMap<>();
                nuevoGrupo.put("grupo", grupoActual);
                nuevoGrupo.put("opciones", new ArrayList<>(opcionesFila));

                grupos.add(nuevoGrupo);
            }
        }

        Map<String, Object> modeloIA = new LinkedHashMap<>();

        modeloIA.put("tipo", "CHECKLIST");
        modeloIA.put("tabla", modeloChecklistGenerico.get("tabla"));
        modeloIA.put("grupos", grupos);
        modeloIA.put("_mapaCoordenadas", mapaInterno);

        return modeloIA;
    }

    private boolean esTablaMatrizEditable(
            List<Map<String, Object>> bloque,
            int anchoTotalTabla
    ) {

        if (bloque == null || bloque.size() < 2) return false;

        // 1️⃣ Debe tener encabezado con múltiples columnas
        List<Map<String, Object>> header =
                (List<Map<String, Object>>) bloque.get(0).get("celdas");

        if (header == null || header.size() < 2) return false;

        long columnasConTexto = header.stream()
                .filter(c -> {
                    String t = (String) c.get("texto");
                    return t != null && !t.isBlank();
                })
                .count();

        if (columnasConTexto < 2) return false;

        // 2️⃣ Debe tener al menos una fila con varias celdas vacías editables
        int filasConCampos = 0;

        for (int i = 1; i < bloque.size(); i++) {

            List<Map<String, Object>> fila =
                    (List<Map<String, Object>>) bloque.get(i).get("celdas");

            if (fila == null) continue;

            long vacias = fila.stream()
                    .filter(c -> Boolean.TRUE.equals(c.get("esVacia")))
                    .count();

            if (vacias >= 2) filasConCampos++;
        }

        return filasConCampos >= 1;
    }

}
