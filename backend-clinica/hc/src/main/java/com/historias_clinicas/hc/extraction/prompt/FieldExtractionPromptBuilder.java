package com.historias_clinicas.hc.extraction.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.extraction.dto.NaturalLanguageFillRequest;

public class FieldExtractionPromptBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String build(NaturalLanguageFillRequest request) {

        try {

            String camposJson = mapper.writeValueAsString(request.getCampos());

            return """
Eres un sistema clínico de extracción de datos.

Tu tarea es EXTRAER valores explícitos desde el texto clínico
y asignarlos a campos definidos.

============================================================
📌 REGLAS ABSOLUTAS
============================================================

- SOLO extrae información EXPLÍCITA en el texto
- NO inferir
- NO asumir
- NO completar
- NO interpretar clínicamente

- Si un campo no aparece claramente → NO devolverlo
- Si hay ambigüedad → NO devolverlo

============================================================
📌 NEGACIONES (CRÍTICO)
============================================================

Si el texto contiene negaciones como:
- "niega"
- "sin"
- "no presenta"

→ NO debes devolver ese campo

Ejemplo:
"niega fiebre" → NO devolver fiebre

============================================================
📌 EXTRACCIÓN LIMPIA
============================================================

Debes devolver SOLO el valor limpio:

- numero → solo número
  "45 años" → "45"

- fecha → formato YYYY-MM-DD si es posible

- booleano → "true" o "false"

- texto → sin palabras innecesarias

============================================================
📌 CAMPOS BOOLEANOS
============================================================

Si el campo representa una selección entre opciones:

- Debes devolver "true" SOLO si el valor del campo aparece explícitamente
- No devolver el texto de la opción

Ejemplo:
Texto: "anamnesis directa"

→ campo "anamnesis directa" → "true"
→ no devolver "directa" como texto

============================================================
📌 USO DE DESCRIPCIÓN
============================================================

Cada campo tiene una descripción.

Debes usarla como definición semántica exacta del campo.

============================================================
📌 RESTRICCIONES
============================================================

- Usa SOLO stableIds proporcionados
- NO inventes campos
- NO repitas campos
- NO devuelvas valores vacíos

============================================================
📦 FORMATO DE SALIDA
============================================================

Devuelve SOLO JSON:

[
  { "stableId": "...", "valor": "..." }
]

============================================================
📄 TEXTO
============================================================
%s

============================================================
📄 CAMPOS
============================================================
%s
""".formatted(
                    request.getTexto(),
                    camposJson
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}