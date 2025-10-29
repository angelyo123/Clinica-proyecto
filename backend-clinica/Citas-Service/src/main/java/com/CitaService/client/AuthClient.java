/*package com.CitaService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:8081/auth")
public interface AuthClient {
    @GetMapping("/validate")
    Map<String, Object> validateToken(@RequestHeader("Authorization") String token);
}*/