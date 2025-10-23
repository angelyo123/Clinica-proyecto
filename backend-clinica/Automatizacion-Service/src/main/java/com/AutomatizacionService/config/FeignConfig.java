package com.AutomatizacionService.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken tokenAuth) {
            Object credentials = tokenAuth.getCredentials();
            if (credentials instanceof String token && token.startsWith("ey")) {
                template.header("Authorization", "Bearer " + token);
            }
        }
    }
}