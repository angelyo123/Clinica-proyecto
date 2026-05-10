package com.historias_clinicas.hc.extraction;

import com.historias_clinicas.hc.extraction.dto.NaturalLanguageFillRequest;

public class FieldExtractionPromptBuilder {

    public static String build(NaturalLanguageFillRequest request) {

        return """
Eres un sistema médico que extrae valores de texto.

Debes leer el texto y asignar valores solo a los campos que correspondan.

Reglas:

- Usa solo stableIds existentes
- No inventes campos
- Si un campo no aparece en el texto no lo devuelvas
- Respeta el tipoDato

Texto:
%s

Campos disponibles:
%s

Devuelve SOLO JSON:

[
 {
   "stableId": "...",
   "valor": "..."
 }
]
""".formatted(
                request.getTexto(),
                toJson(request.getCampos())
        );
    }

}
