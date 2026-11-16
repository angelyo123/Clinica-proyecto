package com.historias_clinicas.hc.extraction.service;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.extraction.dto.FieldValueAI;
import com.historias_clinicas.hc.extraction.dto.NaturalLanguageFillRequest;
import com.historias_clinicas.hc.extraction.prompt.FieldExtractionPromptBuilder;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class FieldExtractionService {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public FieldExtractionService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public List<FieldValueAI> extract(String texto, List<FieldDefinition> campos) {

        NaturalLanguageFillRequest req = new NaturalLanguageFillRequest();
        req.setTexto(texto);
        req.setCampos(campos);

        String prompt = FieldExtractionPromptBuilder.build(req);

        String response = deepSeekClient.call(prompt);

        log.info("==== PROMPT IA ====");
        log.debug(prompt);

        log.info("==== RESPUESTA IA RAW ====");
        log.info(response);

        response = response
                .replace("```json", "")
                .replace("```", "")
                .trim();

        try {

            List<FieldValueAI> result = mapper.readValue(response, new TypeReference<List<FieldValueAI>>() {});

            log.info("==== VALORES IA PARSEADOS ====");

            for (FieldValueAI v : result) {
                log.info("{} -> {}", v.getStableId(), v.getValor());
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}