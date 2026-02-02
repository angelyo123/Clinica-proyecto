package com.historias_clinicas.hc.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextInterpreter {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> interpretarTexto(
            String texto,
            List<Map<String, Object>> camposPlantilla
    ) {

        Map<String, Object> payload = Map.of(
                "texto", texto,
                "campos", camposPlantilla
        );

        String raw = deepSeekClient
                .completarJSON_sinValidar(payload, DeepSeekClient.IA_TEXT_FILLER_PROMPT);

        String sane = sanitize(raw);

        try {
            return mapper.readValue(sane, Map.class);
        } catch (Exception e) {
            log.error("❌ Error parseando JSON IA", e);
            return Map.of();
        }
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "{}";

        int i = raw.indexOf("{");
        int j = raw.lastIndexOf("}");

        if (i < 0 || j <= i) return "{}";

        return raw.substring(i, j + 1)
                .replaceAll("[\\u0000-\\u001F]", "")
                .replaceAll(",\\s*}", "}");
    }
}