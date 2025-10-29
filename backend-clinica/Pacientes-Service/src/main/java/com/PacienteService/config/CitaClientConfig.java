package com.PacienteService.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CitaClientConfig {

    @Bean("citaRequestInterceptor")
    public RequestInterceptor citaRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                System.out.println("🔹 CitaClient: NO agregando headers");
            }
        };
    }
}
