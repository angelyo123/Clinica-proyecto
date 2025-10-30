package com.AutomatizacionService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.tcp.SslProvider;

import reactor.netty.http.client.HttpClient;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private final WebClient webClient;

    public DeepSeekService(@Value("${DEEPSEEK_API_KEY}") String apiKey) {

        HttpClient httpClient = HttpClient.create()
                .secure(SslProvider.defaultClientProvider());

        this.webClient = WebClient.builder()
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .baseUrl("https://api.deepseek.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public String generarTexto(String prompt) {
        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "system", "content", "Eres un asistente útil y conciso."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("✅ DeepSeek respondió correctamente");
            return response;

        } catch (WebClientResponseException e) {
            System.err.println("❌ DeepSeek error HTTP " + e.getRawStatusCode() + ": " + e.getResponseBodyAsString());
            return "{\"error\":\"Respuesta HTTP " + e.getRawStatusCode() + "\"}";
        } catch (Exception e) {
            System.err.println("⚠️ DeepSeek conexión fallida: " + e.getMessage());
            // fallback para seguir operando
            return """
                    {
                      "intencion": "crear_cita",
                      "especialidad": "Cardiología",
                      "fecha": "2025-10-29",
                      "hora": "10:00"
                    }
                    """;
        }
    }
}
