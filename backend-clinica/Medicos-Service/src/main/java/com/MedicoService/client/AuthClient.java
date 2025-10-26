package com.MedicoService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:8081/auth")
public interface AuthClient {

    @PostMapping("/register/medico")
    Map<String, Object> registrarUsuarioMedico(@RequestBody Map<String, Object> data);
}