package com.horariosservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "horarios-service", url = "http://localhost:8085/horarios")
public interface HorarioClient {
    @GetMapping("/medico/{id}")
    List<Map<String, Object>> listarPorMedico(@PathVariable("id") Long medicoId);
}