package com.AutomatizacionService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;


@FeignClient(name = "horarios-service", url = "http://localhost:8085/horarios",
        configuration = com.AutomatizacionService.config.FeignConfig.class)
public interface HorarioClient {

    @GetMapping("/medico/{id}")
    List<Map<String, Object>> listarPorMedico(@PathVariable Long id);
}
