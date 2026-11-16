package com.historias_clinicas.hc.extraction.service;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.extraction.dto.FieldValueAI;
import com.historias_clinicas.hc.extraction.validation.FieldValueValidator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NaturalLanguageFillService {

    private final FieldExtractionService extractionService;

    public NaturalLanguageFillService(FieldExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    public List<FieldValueAI> process(String texto, List<FieldDefinition> campos) {

        // llamar IA
        List<FieldValueAI> aiValues =
                extractionService.extract(texto, campos);

        // validar resultados
        return FieldValueValidator.validate(aiValues, campos);
    }
}