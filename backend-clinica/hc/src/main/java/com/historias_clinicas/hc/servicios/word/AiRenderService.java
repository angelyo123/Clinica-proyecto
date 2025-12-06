package com.historias_clinicas.hc.servicios.word;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiRenderService {

    private final DeepSeekClient deepSeek;
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> generarInstruccionCelda(
            String textoCeldaReal,
            List<Map<String, Object>> items
    ) {
        try {

            Map<String, Object> payload = Map.of(
                    "textoCeldaReal", textoCeldaReal,
                    "items", items
            );

            String raw = deepSeek.completarJSON_sinValidar(payload, PROMPT);
            String json = extraerJSON(raw);

            return mapper.readValue(json, Map.class);

        } catch (Exception e) {

            // fallback básico
            return Map.of(
                    "strategy", "rewrite",
                    "textoFinal", textoCeldaReal
            );
        }
    }

    private String extraerJSON(String raw) {
        if (raw == null) return "{}";

        raw = raw.replace("```json", "")
                .replace("```", "")
                .trim();

        int a = raw.indexOf("{");
        int b = raw.lastIndexOf("}");

        if (a >= 0 && b > a) {
            return raw.substring(a, b + 1);
        }

        return "{}";
    }

    private static final String PROMPT = """
Eres un editor experto de formularios clínicos médicos.

RECIBES:
{
  "textoCeldaReal": "texto EXACTO de la celda en el formulario",
  "items": [
    {
      "itemIndex": <int>,
      "tipo": "checkbox | texto | celda_llenable",
      "descripcion": "qué representa el item",
      "valor": "marcado | no | <texto>"
    }
  ]
}

REGLAS ESENCIALES:

1) Para cada item:
   - Si tipo == "celda_llenable":
        • Si valor es nulo → dejar vacío
        • Si valor tiene texto → devolver:
            {
              "strategy": "texto_plano",
              "textoFinal": valor
            }
        • NO usar "rewrite".
        • NO necesitar texto original; basta el valor.

2) Para items checkbox:
   - Reemplazar cada "(  )" en orden.
   - marcado → "(X)"
   - no → "( )"

3) Para campos de texto:
   - Reemplazar solo el valor luego de la etiqueta.
   - Mantener todo lo demás sin cambios.

4) NO inventar texto, NO reescribir títulos, NO agregar nada nuevo.

SALIDA:
{
  "strategy": "rewrite" | "texto_plano",
  "textoFinal": "contenido EXACTO que debe escribirse"
}

Nada fuera del JSON.
""";
}
