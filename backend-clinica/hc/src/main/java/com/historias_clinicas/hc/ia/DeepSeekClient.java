package com.historias_clinicas.hc.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeepSeekClient {


    private final WebClient deepSeekClient;
    private final RestTemplate deepSeekRestTemplate;

    @Value("${deepseek.api-key}")
    private String apiKey;

    // ============================================================
    // 1. COMPLETAR TEXTO → prompts pequeños
    // ============================================================
    public String completar(String prompt) {

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        return deepSeekClient.post()
                .uri("/chat/completions")
                .headers(h -> h.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    var choices = (List<Map<String, Object>>) resp.get("choices");
                    var message = (Map<String, Object>) choices.get(0).get("message");
                    return message.get("content").toString();
                })
                .block();
    }


    // ============================================================
    // 2. COMPLETAR IMAGEN (VISION)
    // ============================================================
    public String completarImagen(String base64, String instrucciones) {

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of("type", "text", "text", instrucciones),
                                        Map.of("type", "image", "image_url", "data:image/jpeg;base64," + base64)
                                )
                        )
                )
        );

        return deepSeekClient.post()
                .uri("/chat/completions")
                .headers(h -> h.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    var choices = (List<Map<String, Object>>) resp.get("choices");
                    var message = (Map<String, Object>) choices.get(0).get("message");
                    return message.get("content").toString();
                })
                .block();
    }


    // ============================================================
    // 3. COMPLETAR JSON GIGANTE (TEXTO + CAMPOS + CELDAS)
    // ============================================================
    /**
     * Envía:
     *  prompt → instrucciones pequeñas
     *  input JSON → texto + campos + celdas completas
     */
    public String completarJSON(String prompt, Map<String, Object> inputJson) {

        ObjectMapper mapper = new ObjectMapper();
        String jsonPretty;

        try {
            jsonPretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputJson);
        } catch (Exception e) {
            jsonPretty = inputJson.toString();
        }

        String finalPrompt =
                prompt +
                        "\n\nIMPORTANTE: Debes devolver SOLO json válido.\n" +
                        "Ejemplo del formato esperado:\n" +
                        "{ \"campo\": \"valor\" }\n\n" +
                        "Aquí está el JSON de entrada:\n" +
                        jsonPretty;

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 4096,
                "messages", List.of(
                        Map.of("role", "user", "content", finalPrompt)
                )
        );

        return deepSeekClient.post()
                .uri("/chat/completions")
                .headers(h -> h.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    var choices = (List<Map<String, Object>>) resp.get("choices");
                    var message = (Map<String, Object>) choices.get(0).get("message");
                    return message.get("content").toString();
                })
                .block();
    }


}
