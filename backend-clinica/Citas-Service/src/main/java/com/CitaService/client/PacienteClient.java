package com.CitaService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(
        name = "pacientes-service",
        url = "http://localhost:8082/paciente",
        configuration = com.CitaService.config.FeignConfig.class
)
public interface PacienteClient {
    @GetMapping("/paciente/{id}")
    Map<String, Object> findById(@PathVariable("id") Long id);

    @GetMapping("/public/obtener/{id}")
    Map<String, Object> obtener(@PathVariable Long id);
}