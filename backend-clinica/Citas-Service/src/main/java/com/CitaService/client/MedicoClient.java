package com.CitaService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(
        name = "medicos-service",
        url = "http://localhost:8083/medico",
        configuration = com.CitaService.config.FeignConfig.class
)
public interface MedicoClient {
    @GetMapping("/obtener/{id}")
    Map<String, Object> obtener(@PathVariable Long id);
}