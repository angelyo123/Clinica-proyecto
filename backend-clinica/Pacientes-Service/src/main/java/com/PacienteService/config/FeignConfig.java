package com.PacienteService.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Configuration
public class FeignConfig {

    @Bean
    @ConditionalOnMissingBean(name = "citaRequestInterceptor")  // ✅ No aplicar a CitaClient
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                System.out.println("🔐 PacienteService FeignConfig: Aplicando token");
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null && authentication.getCredentials() instanceof String token) {
                    template.header("Authorization", "Bearer " + token);
                }
            }
        };
    }
}
