package com.historias_clinicas.hc.config;

import com.historias_clinicas.hc.ia.DeepSeekClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DeepSeekConfig {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Bean
    public WebClient deepSeekWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public DeepSeekClient deepSeekClient(
            WebClient deepSeekWebClient,
            RestTemplate deepSeekRestTemplate
    ) {
        return new DeepSeekClient(deepSeekWebClient, deepSeekRestTemplate);
    }
}