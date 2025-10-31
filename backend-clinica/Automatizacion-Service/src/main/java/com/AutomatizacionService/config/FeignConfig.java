package com.AutomatizacionService.config;

import com.AutomatizacionService.service.orquestador.SystemAuthService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;



@Configuration
public class FeignConfig implements RequestInterceptor {

    private final SystemAuthService systemAuthService;

    public FeignConfig(SystemAuthService systemAuthService) {
        this.systemAuthService = systemAuthService;
    }

    @Override
    public void apply(RequestTemplate template) {
        String token = systemAuthService.getSystemToken();

        // 🔐 Añadir cabeceras necesarias
        template.header("Content-Type", "application/json");
        if (token != null && !token.isBlank()) {
            template.header("Authorization", "Bearer " + token);
            System.out.println("🔐 Token IA enviado a Feign: " + token.substring(0, 30) + "...");
        } else {
            System.out.println("⚠️ Token IA ausente al enviar solicitud Feign");
        }
    }
}
