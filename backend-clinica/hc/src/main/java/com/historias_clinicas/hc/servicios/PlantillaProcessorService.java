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
    private final GptVisionClient gptVisionClient;
    private final PlantillaAccionRepo plantillaAccionRepo;
    private final WordToPdfService wordToPdfService;
    private final PdfToPngService pdfToPngService;
    private final PlantillaVisionRepo plantillaVisionRepo;
    private final ExecutorService iaExecutor;

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

    // ============================================================
// ANALIZAR PLANTILLA (POI + GPT-4o + DeepSeek)
// ============================================================
//    @Transactional
//    public Map<String, Object> analizarPlantilla(Long plantillaId) throws Exception {
//
//        long inicio = System.currentTimeMillis();
//        log.info("🧠 Iniciando análisis de plantilla {}", plantillaId);
//
//        Plantilla plantilla = plantillaRepo.findById(plantillaId)
//                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
//
//        String hashEstructura = DigestUtils.sha256Hex(
//                plantilla.getArchivoOriginal()
//        );
//
//        // ♻️ SI YA EXISTE ANÁLISIS → NO REANALIZAR
//        if (plantillaAccionRepo.existsByPlantillaId(plantillaId)) {
//            log.info("♻️ Plantilla {} ya analizada, reutilizando análisis", plantillaId);
//            return cargarAnalisisDesdeBD(plantilla);
//        }
//
//        XWPFDocument doc = new XWPFDocument(
//                new ByteArrayInputStream(plantilla.getArchivoOriginal())
//        );
//
//        limpiarEstructuraAnterior(plantilla);
//
//        // 1️⃣ POI
//        log.info("📄 Extrayendo estructura POI…");
//        Map<String, Object> estructuraPOI = extraerEstructuraCompacta(doc);
//
//        List<?> tablas = (List<?>) estructuraPOI.getOrDefault("tablas", List.of());
//        log.info("📐 POI listo: {} tablas", tablas.size());
//
//        // 2️⃣ Word → PNG → GPT-4o Vision
//        log.info("🖼️ Convirtiendo Word a PNG para análisis visual…");
//
//        byte[] pdf = wordToPdfService.convertirWordAPdf(
//                plantilla.getArchivoOriginal()
//        );
//
//        List<byte[]> pngs = pdfToPngService.convertirTodasLasPaginasAPng(pdf);
//        List<Map<String, Object>> regionesTotales = new ArrayList<>();
//
//        for (int i = 0; i < pngs.size(); i++) {
//
//            String base64 = Base64.getEncoder().encodeToString(pngs.get(i));
//
//            Map<String, Object> visionPagina =
//                    gptVisionClient.analizarImagen(
//                            base64,
//                            VisionPrompts.ANALISIS_VISUAL,
//                            "Analiza esta página de la historia clínica."
//                    );
//
//            Object regionesRaw = visionPagina.get("regiones");
//
//            List<Map<String, Object>> regionesPagina = new ArrayList<>();
//
//            if (regionesRaw instanceof List<?> lista) {
//                regionesPagina.addAll((List<Map<String, Object>>) lista);
//            }
//            else if (regionesRaw instanceof Map<?, ?> map) {
//                regionesPagina.add((Map<String, Object>) map);
//            }
//
//            regionesTotales.addAll(regionesPagina);
//
//        }
//
//// 🔁 Deduplicación estructural segura
//        Set<String> firmas = new HashSet<>();
//        List<Map<String, Object>> regionesUnicas = new ArrayList<>();
//
//        for (Map<String, Object> r : regionesTotales) {
//            String firma =
//                    r.get("ancla_visual") + "|" +
//                            r.get("tipo_region");
//            if (firmas.add(firma)) {
//                regionesUnicas.add(r);
//            }
//        }
//
//        Map<String, Object> vision = Map.of(
//                "regiones_editables", regionesUnicas
//        );
//
//        PlantillaVision pv = new PlantillaVision(
//                null,
//                plantilla,
//                vision, // Map<String,Object>
//                hashEstructura,
//                LocalDateTime.now()
//        );
//
//        plantillaVisionRepo.save(pv);
//
//        log.info("👁️ Vision detectó {} regiones ({} tras deduplicar)",
//                regionesTotales.size(),
//                regionesUnicas.size());
//
//
//        // 3️⃣ DeepSeek conciliador (POR REGIÓN)
//        /*
//        log.info("🤝 Conciliando POI + Vision (por región)…");
//
//        List<Map<String, Object>> accionesTotales = new ArrayList<>();
//
//        List<Map<String, Object>> regiones =
//                (List<Map<String, Object>>) vision.getOrDefault("regiones", List.of());
//
//        for (Map<String, Object> region : regiones) {
//
//            String hint = (String) region.get("hint_text");
//            log.info("🧠 Conciliando región visual: {}", hint);
//
//        }
//        */
//
//        // 3️⃣ DeepSeek: extracción de filas con ":"
//        log.info("🤝 Extrayendo filas editables (:) con DeepSeek…");
//
//        List<Map<String, Object>> accionesTotales =
//                deepSeekClient.conciliarRegion(
//                        estructuraPOI,
//                        Map.of() // region ignorada
//                );
//
//        guardarAccionesComoPlantilla(
//                accionesTotales,
//                plantilla,
//                hashEstructura
//        );
//
//
//        return Map.of(
//                "estructuraPOI", estructuraPOI,
//                "vision", vision
//        );
//    }


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

    log.info("📐 POI listo: {} tablas, {} párrafos", tablas.size(), parrafos.size());

    // ============================================================
    // 2️⃣ VISIÓN – CONTEXTO SEMÁNTICO (NO OBLIGATORIO)
    // ============================================================
    log.info("👁️ Ejecutando análisis visual (Vision)…");

    byte[] pdf = wordToPdfService.convertirWordAPdf(
            plantilla.getArchivoOriginal()
    );

    List<byte[]> pngs = pdfToPngService.convertirTodasLasPaginasAPng(pdf);

    List<Map<String, Object>> regionesTotales = new ArrayList<>();

    for (int i = 0; i < pngs.size(); i++) {

        String base64 = Base64.getEncoder().encodeToString(pngs.get(i));

        Map<String, Object> visionPagina =
                gptVisionClient.analizarImagen(
                        base64,
                        VisionPrompts.ANALISIS_VISUAL,
                        "Analiza esta página del documento."
                );

        Object regionesRaw = visionPagina.get("regiones");
        log.info("👁️ Vision RAW keys: {}", visionPagina.keySet());
        if (regionesRaw instanceof List<?> lista) {
            regionesTotales.addAll((List<Map<String, Object>>) lista);
        }
    }

    // 🔁 Deduplicación visual por ancla + tipo
    Set<String> firmas = new HashSet<>();
    List<Map<String, Object>> regionesUnicas = new ArrayList<>();

    for (Map<String, Object> r : regionesTotales) {
        String firma = r.get("ancla_visual") + "|" + r.get("tipo_region");
        if (firmas.add(firma)) {
            regionesUnicas.add(r);
        }
    }

    Map<String, Object> visionContext = Map.of(
            "regiones_editables", regionesUnicas
    );

    log.info("👁️ Vision detectó {} regiones ({} únicas)",
            regionesTotales.size(), regionesUnicas.size());

    // ============================================================
    // 3️⃣ DEEPSEEK EN PARALELO (TABLAS + VISIÓN)
    // ============================================================
    log.info("⚡ Extrayendo campos editables con DeepSeek (paralelo)…");

    List<Future<List<Map<String, Object>>>> futures = new ArrayList<>();

    for (Map<String, Object> tabla : tablas) {

        futures.add(
                iaExecutor.submit(() -> {

                    Map<String, Object> poiParcial = Map.of(
                            "tablas", List.of(tabla)
                    );

                    return deepSeekClient.conciliarRegion(
                            poiParcial,
                            visionContext   // 👈 VISIÓN COMO AYUDA
                    );
                })
        );
    }

    // ============================================================
    // 3️⃣B DEEPSEEK PARA PÁRRAFOS (CON VISIÓN)
    // ============================================================
    if (!parrafos.isEmpty()) {

        log.info("🧠 Procesando párrafos como unidades editables…");

        futures.add(
                iaExecutor.submit(() -> {

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
                            visionContext   // 👈 VISIÓN TAMBIÉN AQUÍ
                    );
                })
        );
    }

    // ============================================================
    // 4️⃣ RECOLECCIÓN DE RESULTADOS
    // ============================================================
    List<Map<String, Object>> accionesTotales = new ArrayList<>();

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

            log.error("💥 Error en tarea IA: {}", root.getMessage(), root);
        }
    }

    log.info("🧾 Total de campos detectados: {}", accionesTotales.size());

    // ============================================================
    // 5️⃣ GUARDADO
    // ============================================================
    guardarAccionesComoPlantilla(
            accionesTotales,
            plantilla,
            hashEstructura
    );

    return Map.of(
            "origen", "POI+VISION+DEEPSEEK+THREADS",
            "plantillaId", plantillaId,
            "duracion_ms", System.currentTimeMillis() - inicio,
            "accionesDetectadas", accionesTotales.size(),
            "vision", visionContext
    );
}
    private void guardarAccionesComoPlantilla(
            List<Map<String, Object>> acciones,
            Plantilla plantilla,
            String hashEstructura
    ) {

        for (Map<String, Object> a : acciones) {


            /*
            String tipoRegionRaw =
                    ((String) a.get("tipo_region"))
                            .trim()
                            .toUpperCase();


            if (!EnumUtils.isValidEnum(TipoRegion.class, tipoRegionRaw)) {
                throw new IllegalStateException(
                        "Tipo de región inválido: " + tipoRegionRaw
                );
            }*/

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


    // ============================================================
    // EXTRACTOR POI (ESTRUCTURA CANÓNICA)
    // ============================================================
    private Map<String, Object> extraerEstructuraCompacta(XWPFDocument doc) {

        Map<String, Object> data = new LinkedHashMap<>();

        // Párrafos
        List<Map<String, Object>> parrafos = new ArrayList<>();
        int p = 0;
        for (XWPFParagraph parrafo : doc.getParagraphs()) {
            String txt = Optional.ofNullable(parrafo.getText()).orElse("").trim();
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

        // Tablas
        List<Map<String, Object>> tablas = new ArrayList<>();
        int t = 0;

        for (XWPFTable tabla : doc.getTables()) {

            List<Map<String, Object>> filas = new ArrayList<>();
            int maxCols = tabla.getRows().stream()
                    .mapToInt(r -> r.getTableCells().size())
                    .max().orElse(0);

            int f = 0;
            for (XWPFTableRow row : tabla.getRows()) {

                List<Map<String, Object>> celdas = new ArrayList<>();
                List<XWPFTableCell> reales = row.getTableCells();

                for (int c = 0; c < maxCols; c++) {
                    XWPFTableCell celda = c < reales.size() ? reales.get(c) : null;
                    String texto = celda != null ? celda.getText().replace("\n", "").trim() : "";

                    celdas.add(Map.of(
                            "tabla", t,
                            "fila", f,
                            "columna", c,
                            "texto", texto
                    ));
                }

                filas.add(Map.of("fila", f, "celdas", celdas));
                f++;
            }

            tablas.add(Map.of("tabla", t, "filas", filas));
            t++;
        }

        data.put("tablas", tablas);
        return data;
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

    // ============================================================
    // LIMPIEZA
    // ============================================================
    private void limpiarEstructuraAnterior(Plantilla plantilla) {

        seccionRepo.deleteByPlantillaId(plantilla.getId());
        plantillaAccionRepo.deleteByPlantillaId(plantilla.getId());

        log.info("🧹 Estructura anterior eliminada (secciones + acciones)");
    }
}
