package com.historias_clinicas.hc.ia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RequiredArgsConstructor
public class DeepSeekClient {
    private final WebClient deepSeekClient;

    public String completar(String prompt) {
        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", new Object[]{
                        Map.of("role", "user", "content", prompt)
                }
        );

        return deepSeekClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    var choices = (java.util.List<Map<String, Object>>) resp.get("choices");
                    var first = choices.get(0);
                    var message = (Map<String, Object>) first.get("message");
                    return message.get("content").toString();
                })
                .block();
    }
}