package com.historias_clinicas.hc.ia.cognitive;

import com.historias_clinicas.hc.dto.CognitiveCell;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CognitivePromptBuilder {

    public String buildPrompt(List<CognitiveCell> cells) {

        return """
        Eres un analizador estructural de documentos.

        Solo puedes seleccionar stableIds existentes.
        No puedes inventar IDs.
        Devuelve solo JSON válido.

        Documento estructural:
        """ + toJson(cells);
    }
}