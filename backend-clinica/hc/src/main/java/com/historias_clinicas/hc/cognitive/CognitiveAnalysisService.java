package com.historias_clinicas.hc.cognitive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.dto.CognitiveCell;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import com.historias_clinicas.hc.visual.VisualDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CognitiveAnalysisService {

    private final CognitiveSerializer serializer;
    private final CognitivePromptBuilder promptBuilder;
    private final DeepSeekClient iaClient;

    private TargetRegion targetRegion;

    public static final String MODEL_VERSION = "cognitive-v1";

    public List<FieldDefinition> analizar(VisualDocument doc) {

        List<CognitiveCell> cells = serializer.serialize(doc);

        List<List<CognitiveCell>> bloques = dividirEnBloques(cells);

        List<FieldDefinition> allFields = new ArrayList<>();

        int i = 0;

        for (List<CognitiveCell> bloque : bloques) {

            i++;
            log.info("🧠 Analizando bloque {}", i);

            String prompt = promptBuilder.buildPrompt(bloque);

            String respuesta = iaClient.call(prompt);

            log.info("===== RESPUESTA IA RAW =====");
            log.info(respuesta);

            List<FieldDefinition> fields = parseRespuesta(respuesta);

            log.info("===== FIELDS PARSEADOS =====");

            for (FieldDefinition f : fields) {

                log.info("🔎 VALIDANDO FIELD");
                log.info("stableId: {}", f.getStableId());
                log.info("writeStableId: {}", f.getWriteStableId());
                log.info("writeMode: {}", f.getWriteMode());

                if (f.getTargetRegion() != null) {
                    log.info("targetRegion.mode: {}", f.getTargetRegion().getMode());
                } else {
                    log.info("targetRegion: null");
                }

                log.info("FIELD -> readId: {}, writeId: {}, mode: {}, region: {}",
                        f.getStableId(),
                        f.getWriteStableId(),
                        f.getWriteMode(),
                        f.getTargetRegion() != null ? f.getTargetRegion().getMode() : "null"
                );
            }

            validate(fields, bloque);

            allFields.addAll(fields);
        }

        return deduplicar(allFields);
    }


    private List<FieldDefinition> deduplicar(List<FieldDefinition> list) {

        Map<String, FieldDefinition> map = new LinkedHashMap<>();

        for (FieldDefinition f : list) {
            map.put(f.getStableId(), f);
        }

        return new ArrayList<>(map.values());
    }

    private void validate(
            List<FieldDefinition> fields,
            List<CognitiveCell> cells
    ) {

        log.info("===== INICIANDO VALIDACION =====");
        log.info("Total fields: {}", fields.size());

        Set<String> validIds = cells.stream()
                .map(CognitiveCell::getStableId)
                .collect(Collectors.toSet());

        Set<String> tiposValidos = Set.of("texto", "numero", "fecha", "booleano");


        Set<String> interactionTypes = Set.of(
                "WRITE_TEXT",
                "APPEND_INLINE",
                "WRITE_BELOW",
                "TOGGLE_MARK",
                "SELECT_OPTION",
                "WRITE_MULTILINE"
        );

        Set<String> preservationModes = Set.of(
                "PRESERVE_EXISTING",
                "REPLACE_CONTENT",
                "MERGE_WITH_EXISTING"
        );

        Set<String> semantics = Set.of(
                "SLOT",
                "LABEL_WITH_SLOT",
                "OPTION_CONTROL",
                "WHOLE_CELL",
                "FREE_TEXT_AREA"
        );

        Set<String> writeModes = Set.of("TEXT", "MARK", "REWRITE");

        Map<String, List<FieldDefinition>> writes = new HashMap<>();

        for (FieldDefinition f : fields) {

            if (f.getStableId() == null || f.getStableId().isBlank())
                throw new RuntimeException("stableId vacío");

            if (f.getWriteStableId() == null || f.getWriteStableId().isBlank())
                throw new RuntimeException("writeStableId vacío");

            if (!validIds.contains(f.getStableId()))
                throw new RuntimeException("IA inventó stableId");

            if (!validIds.contains(f.getWriteStableId()))
                throw new RuntimeException("IA inventó writeStableId");

            if (!tiposValidos.contains(f.getDataType()))
                throw new RuntimeException("Tipo de dato inválido: " + f.getDataType());

            writes
                    .computeIfAbsent(f.getWriteStableId(), k -> new ArrayList<>())
                    .add(f);

            if (f.getStableId().equals(f.getWriteStableId())) {
                log.debug("Campo inline: {}", f.getStableId());
            } else {
                log.debug("Campo separado: {} -> {}", f.getStableId(), f.getWriteStableId());
            }

            if (!interactionTypes.contains(f.getInteractionType()))
                throw new RuntimeException("interactionType inválido");

            if (!preservationModes.contains(f.getPreservationMode()))
                throw new RuntimeException("preservationMode inválido");

            if (!semantics.contains(f.getTargetSemantics()))
                throw new RuntimeException("targetSemantics inválido");

            if (!writeModes.contains(f.getWriteMode()))
                throw new RuntimeException("writeMode inválido");

            if (!"REWRITE".equals(f.getWriteMode()) && f.getTargetRegion() == null) {
                throw new RuntimeException("targetRegion obligatorio para TEXT o MARK");
            }

            Set<String> regionModes = Set.of(
                    "WHOLE_CELL",
                    "INLINE_AFTER_TEXT",
                    "INLINE_BEFORE_TEXT",
                    "BELOW_LABEL",
                    "INSIDE_EXISTING_MARK",
                    "BETWEEN_TEXTS",
                    "RELATIVE_POSITION"
            );

            if (!"REWRITE".equals(f.getWriteMode())) {

                if (f.getTargetRegion() == null)
                    throw new RuntimeException("targetRegion obligatorio");

                String mode = f.getTargetRegion() != null ? f.getTargetRegion().getMode() : null;


                log.error("❌ MODE INVALIDO DETECTADO");
                log.error("writeMode: {}", f.getWriteMode());
                log.error("targetRegion.mode: {}", mode);

                if (!regionModes.contains(mode))
                    throw new RuntimeException("targetRegion.mode inválido: " + mode);

            }
        }

        for (Map.Entry<String, List<FieldDefinition>> e : writes.entrySet()) {

            List<FieldDefinition> sameTarget = e.getValue();

            if (sameTarget.size() <= 1) continue;

            log.warn("⚠️ Múltiples campos escriben en {}", e.getKey());

            validateMultiWrite(sameTarget);
        }


    }


    private void validarConflictos(List<FieldDefinition> fields) {

        long replaceCount = fields.stream()
                .filter(f -> "REPLACE_CONTENT".equals(f.getPreservationMode()))
                .count();

        if (replaceCount > 1) {
            throw new RuntimeException(
                    "Conflicto: múltiples REPLACE_CONTENT en misma celda"
            );
        }

        // opcional: logs para debug
        for (FieldDefinition f : fields) {
            log.debug("→ {} [{}]",
                    f.getStableId(),
                    f.getInteractionType());
        }
    }

    private void validateMultiWrite(List<FieldDefinition> fields) {

        Set<String> interactionTypes = fields.stream()
                .map(FieldDefinition::getInteractionType)
                .collect(Collectors.toSet());

        // 🔥 REGLA 1: NO permitir múltiples REPLACE
        long replaceCount = fields.stream()
                .filter(f -> f.getPreservationMode().equals("REPLACE_CONTENT"))
                .count();

        if (replaceCount > 1) {
            throw new RuntimeException(
                    "Conflicto: múltiples REPLACE_CONTENT en misma celda"
            );
        }

        // 🔥 REGLA 2: permitir combinación segura
        // APPEND_INLINE + WRITE_TEXT → OK
        // WRITE_BELOW múltiples → OK
    }

    private List<FieldDefinition> parseRespuesta(String raw) {


        try {

            String limpio = raw
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> root =
                    mapper.readValue(limpio, Map.class);

            Object fieldsObj = root.get("fields");

            if (!(fieldsObj instanceof List<?> list))
                throw new RuntimeException("IA no devolvió lista de fields");


            List<FieldDefinition> result = new ArrayList<>();


            for (Object o : list) {


                if (!(o instanceof Map<?, ?> m))
                    continue;

                log.debug("RAW FIELD MAP: {}", m);

                FieldDefinition f = new FieldDefinition();

                f.setStableId((String) m.get("stableId"));
                f.setDescription((String) m.get("description"));
                f.setDataType((String) m.get("dataType"));
                f.setWriteStableId((String) m.get("writeStableId"));

                f.setInteractionType((String) m.get("interactionType"));
                f.setPreservationMode((String) m.get("preservationMode"));
                f.setTargetSemantics((String) m.get("targetSemantics"));

                Object regionObj = m.get("targetRegion");

                if (regionObj instanceof Map<?, ?> rm) {

                    TargetRegion tr = new TargetRegion();

                    tr.setMode((String) rm.get("mode"));
                    tr.setAnchorText((String) rm.get("anchorText"));
                    tr.setSecondAnchorText((String) rm.get("secondAnchorText"));

                    if (rm.get("relativeRow") != null)
                        tr.setRelativeRow((Integer) rm.get("relativeRow"));

                    if (rm.get("relativeCol") != null)
                        tr.setRelativeCol((Integer) rm.get("relativeCol"));

                    tr.setReference((String) rm.get("reference"));
                    tr.setHint((String) rm.get("hint"));

                    f.setTargetRegion(tr);

                    f.setWriteMode((String) m.get("writeMode"));
                    f.setWriteValue((String) m.get("writeValue"));
                    f.setRewriteText((String) m.get("rewriteText"));
                }

                if (f.getWriteStableId() == null || f.getWriteStableId().isBlank())
                    throw new RuntimeException("IA no devolvió writeStableId");

                result.add(f);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Error parseando respuesta IA", e);
        }
    }

    private List<List<CognitiveCell>> dividirEnBloques(List<CognitiveCell> cells) {

        Map<Integer, List<CognitiveCell>> porBloque = new LinkedHashMap<>();

        for (CognitiveCell c : cells) {
            porBloque
                    .computeIfAbsent(c.getBodyIndex(), k -> new ArrayList<>())
                    .add(c);
        }

        return new ArrayList<>(porBloque.values());
    }
}