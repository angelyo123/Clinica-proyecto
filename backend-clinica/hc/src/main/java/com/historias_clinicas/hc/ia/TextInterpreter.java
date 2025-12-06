package com.historias_clinicas.hc.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextInterpreter {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Interpreta texto clínico escrito por un médico
     * y devuelve valores para los campos de la plantilla.
     */
    public Map<String, Object> interpretarTexto(
            String texto,
            List<Map<String, Object>> camposPlantilla
    ) throws Exception {

        Map<String, Object> payload = Map.of(
                "texto", texto,
                "campos", camposPlantilla
        );

        log.info("➡️ Enviando a IA: {}", payload);

        String raw = deepSeekClient.completarJSON_sinValidar(payload, DeepSeekClient.IA_TEXT_FILLER_PROMPT);

        log.info("⬅️ Respuesta IA RAW: {}", raw);

        String sane = sanitize(raw);

        return mapper.readValue(sane, Map.class);
    }

    // Sanitizador JSON básico
    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "{}";

        String s = raw.trim();
        int i = s.indexOf("{");
        int j = s.lastIndexOf("}");

        if (i < 0 || j <= i) {
            log.error("❌ IA no devolvió JSON válido");
            return "{}";
        }

        s = s.substring(i, j + 1);
        s = s.replaceAll("[\\u0000-\\u001F]", "");
        s = s.replaceAll(",\\s*}", "}");
        s = s.replaceAll(",\\s*]", "]");

        return s;
    }
}