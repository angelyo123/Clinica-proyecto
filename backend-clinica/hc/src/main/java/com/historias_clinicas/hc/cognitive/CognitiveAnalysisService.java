package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.dto.CognitiveCell;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import com.historias_clinicas.hc.cognitive.CognitivePromptBuilder;
import com.historias_clinicas.hc.cognitive.CognitiveSerializer;
import com.historias_clinicas.hc.visual.VisualDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CognitiveAnalysisService {

    private final CognitiveSerializer serializer;
    private final CognitivePromptBuilder promptBuilder;
    private final DeepSeekClient iaClient;

    public List<FieldDefinition> analizar(VisualDocument doc) {

        List<CognitiveCell> cells = serializer.serialize(doc);

        String prompt = promptBuilder.buildPrompt(cells);

        String respuesta = iaClient.call(prompt);

        List<FieldDefinition> fields = parseRespuesta(respuesta);

        validate(fields, cells);

        return fields;
    }

    private void validate(
            List<FieldDefinition> fields,
            List<CognitiveCell> cells
    ) {

        Set<String> validIds = cells.stream()
                .map(CognitiveCell::getStableId)
                .collect(Collectors.toSet());

        for (FieldDefinition f : fields) {

            if (!validIds.contains(f.getStableId())) {
                throw new RuntimeException("IA inventó stableId");
            }
        }

        if (fields.size() > validIds.size() * 0.4) {
            throw new RuntimeException("IA marcó demasiados campos");
        }
    }
}