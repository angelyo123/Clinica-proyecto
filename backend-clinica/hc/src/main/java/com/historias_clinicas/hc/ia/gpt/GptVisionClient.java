package com.historias_clinicas.hc.ia.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GptVisionClient {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.api-url}")
    private String apiUrl;

    @Value("${openai.model.vision}")
    private String model;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * GPT-4o Vision
     * Devuelve SIEMPRE:
     * {
     *   "regiones": [ ... ]
     * }
     */
    public Map<String, Object> analizarImagen(
            String base64Image,
            String promptSistema,
            String promptUsuario
    ) {

        try {

            Map<String, Object> imageContent = Map.of(
                    "type", "image_url",
                    "image_url", Map.of(
                            "url", "data:image/png;base64," + base64Image
                    )
            );

            Map<String, Object> textContent = Map.of(
                    "type", "text",
                    "text", promptUsuario
            );

            Map<String, Object> messageUser = Map.of(
                    "role", "user",
                    "content", List.of(textContent, imageContent)
            );

            Map<String, Object> messageSystem = Map.of(
                    "role", "system",
                    "content", promptSistema
            );

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(messageSystem, messageUser));
            body.put("temperature", 0);
            body.put("max_tokens", 2000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    Map.class
            );

            Map<?, ?> raw = response.getBody();
            if (raw == null) {
                throw new RuntimeException("Respuesta nula de OpenAI Vision");
            }

            Map<?, ?> choice =
                    (Map<?, ?>) ((List<?>) raw.get("choices")).get(0);

            Map<?, ?> message =
                    (Map<?, ?>) choice.get("message");

            Object content = message.get("content");

            // 🔥 NORMALIZACIÓN CLAVE
            List<Map<String, Object>> regiones = extraerRegiones(content);

            log.info("👁️ [Vision] Regiones normalizadas: {}", regiones.size());

            return Map.of("regiones", regiones);

        } catch (Exception e) {
            log.error("❌ Error GPT-4o Vision", e);
            return Map.of(
                    "regiones", List.of(),
                    "error", e.getMessage()
            );
        }
    }

    // ============================================================
    // NORMALIZADOR DE SALIDA VISION
    // ============================================================
    private List<Map<String, Object>> extraerRegiones(Object content)
            throws Exception {

        if (content == null) return List.of();

        if (content instanceof String texto) {

            String limpio = texto
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            if (limpio.isBlank()) return List.of();

            return mapper.readValue(limpio, List.class);
        }

        // Caso raro: Vision devuelve estructura directa
        if (content instanceof List<?>) {
            return (List<Map<String, Object>>) content;
        }

        throw new RuntimeException(
                "Formato inesperado en content Vision: " + content.getClass()
        );
    }
}