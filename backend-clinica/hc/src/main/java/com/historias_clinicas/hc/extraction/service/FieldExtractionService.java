package com.historias_clinicas.hc.extraction;

import com.historias_clinicas.hc.extraction.dto.FieldValueAI;
import com.historias_clinicas.hc.extraction.dto.NaturalLanguageFillRequest;
import com.historias_clinicas.hc.extraction.prompt.FieldExtractionPromptBuilder;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FieldExtractionService {

    private final DeepSeekClient deepSeekClient;

    public FieldExtractionService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public List<FieldValueAI> extract(NaturalLanguageFillRequest request) {

        String prompt = FieldExtractionPromptBuilder.build(request);

        String response = deepSeekClient.call(prompt);

        return JsonUtils.parseList(response, FieldValueAI.class);
    }
}