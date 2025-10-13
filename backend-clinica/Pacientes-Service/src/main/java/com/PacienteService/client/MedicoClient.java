package com.PacienteService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "medicos-service", url = "http://localhost:8083/medico")
public interface MedicoClient {
    @GetMapping("/public/listar")
    List<Map<String, Object>> listar();
}