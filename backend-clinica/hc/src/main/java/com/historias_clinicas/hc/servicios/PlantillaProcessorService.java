package com.historias_clinicas.hc.servicios;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.cognitive.CognitiveAnalysisService;
import com.historias_clinicas.hc.cognitive.CognitiveSerializer;
import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.dto.CognitiveCell;
import com.historias_clinicas.hc.entidades.*;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import com.historias_clinicas.hc.repositorios.*;
import com.historias_clinicas.hc.visual.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaProcessorService {

    private final PlantillaRepository plantillaRepo;
    private final PlantillaSnapshotRepo snapshotRepo;

    private final CognitiveAnalysisService cognitiveAnalysisService;
    private final CognitivePersistenceService cognitivePersistenceService;

    private final CognitiveSerializer cognitiveSerializer;

    private final PlantillaCampoRepository campoRepo;
    private final DeepSeekClient deepSeekClient;

    // ============================================================
    // SUBIR PLANTILLA
    // ============================================================
    public Plantilla procesarPlantilla(byte[] file, String nombre) {

        Plantilla plantilla = Plantilla.builder()
                .nombre(nombre)
                .archivoOriginal(file)
                .build();

        plantillaRepo.save(plantilla);
        return plantilla;
    }

    @Transactional
    public Map<String, Object> analizarPlantilla(Long plantillaId) throws Exception {

        log.info("🧠 Analizando plantilla {}", plantillaId);

        Plantilla plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        // ========================================================
        // 🔐 HASH DOCUMENTO
        // ========================================================
        String docHash = DigestUtils.sha256Hex(
                plantilla.getArchivoOriginal()
        );

        Optional<PlantillaSnapshot> last =
                snapshotRepo.findTopByPlantillaIdOrderByIdDesc(plantillaId);

        if (last.isPresent()
                && last.get().getDocHash().equals(docHash)
                && last.get().getModelVersion()
                .equals(CognitiveAnalysisService.MODEL_VERSION)) {

            log.info("♻️ Snapshot cognitivo ya existe y coincide");
            return Map.of("origen", "BD_SNAPSHOT");
        }

        // ========================================================
        // 🔁 NUEVO ANÁLISIS COGNITIVO
        // ========================================================

        limpiarEstructuraAnterior(plantilla);

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal())
        );

        // 1️⃣ Modelo Visual
        VisualDocumentBuilder builder = new VisualDocumentBuilder();
        VisualDocument visualDocument = builder.build(doc);

        // 2️⃣ Stable IDs
        StableIdGenerator idGenerator = new StableIdGenerator();
        idGenerator.generarIds(visualDocument);

        log.debug("VisualDocument construido correctamente");

        // 3️⃣ IA decide campos
        List<FieldDefinition> fields =
                cognitiveAnalysisService.analizar(visualDocument);

        // 4️⃣ Persistir snapshot congelado
        cognitivePersistenceService.guardarSnapshot(
                plantillaId,
                docHash,
                CognitiveAnalysisService.MODEL_VERSION,
                fields
        );

        log.info("✅ Análisis cognitivo completado. Campos detectados: {}", fields.size());

        return Map.of(
                "plantillaId", plantillaId,
                "camposDetectados", fields.size(),
                "origen", "COGNITIVE_ENGINE"
        );
    }

    // ============================================================
    // LIMPIEZA
    // ============================================================
    private void limpiarEstructuraAnterior(Plantilla plantilla) {

        snapshotRepo.deleteAll(
                snapshotRepo.findAll()
                        .stream()
                        .filter(s -> s.getPlantillaId().equals(plantilla.getId()))
                        .toList()
        );

    }

    @Transactional(readOnly = true)
    public List<String> procesarChecklists(
            Long plantillaId,
            String textoClinico
    ) {

        List<PlantillaCampo> campos =
                campoRepo.findByPlantillaId(plantillaId);

        // Solo booleanos
        List<Map<String, String>> opciones = campos.stream()
                .filter(c -> "booleano".equalsIgnoreCase(c.getTipoDato()))
                .map(c -> Map.of(
                        "stableId", c.getStableId(),
                        "texto", c.getDescripcion()
                ))
                .toList();

        if (opciones.isEmpty()) {
            return List.of();
        }

        Map<String, Object> payload = Map.of(
                "texto_clinico", textoClinico,
                "checklist", Map.of(
                        "tipo", "CHECKLIST",
                        "opciones", opciones
                )
        );

        String respuesta =
                deepSeekClient.completarJSON_sinValidar(
                        payload,
                        DeepSeekClient.IA_CHECKLIST_PROMPT
                );

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed =
                    mapper.readValue(respuesta, Map.class);

            Object marcarObj = parsed.get("marcar");

            if (!(marcarObj instanceof List<?> lista)) {
                return List.of();
            }

            return lista.stream()
                    .map(String::valueOf)
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Error procesando checklist IA", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> llamarDeepSeekChecklist(
            Map<String, Object> payload
    ) {
        // Aquí va tu integración real con DeepSeek
        // Ejemplo esperado de respuesta:

    /*
    {
        "marcar": [
            "B3T1R4C2#8fa91a2b",
            "B3T1R5C2#1c92e881"
        ]
    }
    */

        return Map.of("marcar", List.of());
    }



    @Transactional(readOnly = true)
    public Map<String, Object> extraerEstructuraPoi(Long plantillaId) throws Exception {

        Plantilla plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal())
        );

        List<Map<String, Object>> tablas = new ArrayList<>();

        int index = 0;

        for (var table : doc.getTables()) {

            Map<String, Object> tabla = new LinkedHashMap<>();

            tabla.put("tabla", index);
            tabla.put("filas", table.getNumberOfRows());

            List<Map<String, Object>> rows = new ArrayList<>();

            for (int r = 0; r < table.getNumberOfRows(); r++) {

                var row = table.getRow(r);

                Map<String, Object> rowData = new LinkedHashMap<>();
                rowData.put("fila", r);

                List<String> cells = new ArrayList<>();

                row.getTableCells().forEach(cell -> {
                    cells.add(cell.getText());
                });

                rowData.put("celdas", cells);

                rows.add(rowData);
            }

            tabla.put("estructura", rows);

            tablas.add(tabla);

            index++;
        }

        return Map.of(
                "tablas", tablas
        );
    }

    public Map<String,Object> debugCognitivePayload(Long plantillaId) throws Exception {

        Plantilla plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal())
        );

        // 1️⃣ modelo visual
        VisualDocumentBuilder builder = new VisualDocumentBuilder();
        VisualDocument visual = builder.build(doc);

        // 2️⃣ stable ids
        StableIdGenerator idGen = new StableIdGenerator();
        idGen.generarIds(visual);

        // 3️⃣ serializar para IA
        List<CognitiveCell> cells =
                cognitiveSerializer.serialize(visual);

        return Map.of(
                "cantidad", cells.size(),
                "cells", cells
        );
    }
}