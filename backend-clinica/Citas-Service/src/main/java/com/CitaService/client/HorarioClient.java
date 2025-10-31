package com.CitaService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "horarios-service", url = "http://localhost:8085/horario",
        configuration = com.CitaService.config.FeignConfig.class)
public interface HorarioClient {
    @GetMapping("/disponibles/{medicoId}")
    List<Map<String, Object>> listarDisponibles(@PathVariable Long medicoId);
}