package com.historias_clinicas.hc.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TextInterpreter {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * La IA recibe:
     * - el texto manuscrito o dictado
     * - la lista EXACTA de nombres de campos detectados por POI
     *
     * Y devuelve un JSON con esos nombres como claves.
     */
    public Map<String, Object> interpretarTexto(
            String texto,
            List<String> camposPlantilla
    ) throws Exception {

        // Serializamos la lista de campos como texto
        String camposJson = mapper.writeValueAsString(camposPlantilla);

        // Prompt controlado y robusto
        String prompt = """
        Eres un sistema experto en extracción de datos de historias clínicas.

        Debes devolver ÚNICAMENTE un JSON válido, siguiendo exactamente estas reglas:

        REGLAS:
        - Usa EXCLUSIVAMENTE las siguientes claves (no inventes, no cambies nombres):
        %s
        - Si un dato no existe en el texto, devuelve "" (string vacío).
        - NO agregues claves nuevas.
        - NO borres ninguna clave.
        - Todas las claves deben estar en el JSON final.
        - NO escribas explicaciones, NO markdown, NO texto adicional.
        - SOLO devuelve el JSON final.

        FORMATO ESTRICTO:
        {
          "clave1": "valor",
          "clave2": "valor"
        }

        TEXTO A ANALIZAR:
        --------------------
        %s
        --------------------

        Devuelve SOLO el JSON:
        """.formatted(camposJson, texto);

        // Pedimos el JSON a la IA
        String raw = deepSeekClient.completar(prompt);

        System.out.println("\nRAW IA:\n" + raw);

        String clean = sanitize(raw);

        System.out.println("\nSANITIZED JSON:\n" + clean);

        // Convertimos a Map
        return mapper.readValue(clean, Map.class);
    }

    // ============================================================
    // SANITIZADOR: Recorta solo el JSON, no destruye contenido
    // ============================================================
    private String sanitize(String raw) {

        if (raw == null) return "{}";

        String s = raw.trim();

        // Recortar entre { y }
        int i = s.indexOf("{");
        int j = s.lastIndexOf("}");

        if (i >= 0 && j > i) {
            s = s.substring(i, j + 1);
        } else {
            return "{}";
        }

        // Quitar basura externa a las llaves
        s = s.replaceAll("[\\u0000-\\u001F]", "");

        // Arreglar comas colgantes
        s = s.replaceAll(",\\s*}", "}");
        s = s.replaceAll(",\\s*]", "]");

        return s;
    }
}