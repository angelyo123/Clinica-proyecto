package com.PacienteService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "auth-service",
        url = "http://localhost:8081/auth",
        configuration = com.PacienteService.config.FeignConfig.class
)
public interface AuthClient {

    @PostMapping("/register/paciente")
    Map<String, Object> registrarUsuarioPaciente(@RequestBody Map<String, Object> data);
}