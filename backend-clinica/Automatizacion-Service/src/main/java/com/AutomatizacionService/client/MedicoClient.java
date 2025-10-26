package com.AutomatizacionService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "medico-service", url = "http://localhost:8083/medico",
        configuration = com.AutomatizacionService.config.FeignConfig.class)

public interface MedicoClient {
    @GetMapping("/public/listar")
    List<Map<String, Object>> listarMedicos();

    @GetMapping("/public/especialidad")
    List<Map<String, Object>> listarPorEspecialidad(@RequestParam String especialidad);
}
