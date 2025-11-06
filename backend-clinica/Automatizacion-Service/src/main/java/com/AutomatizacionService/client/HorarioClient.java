package com.AutomatizacionService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;


@FeignClient(name = "horarios-service", url = "http://localhost:8085/horarios",
        configuration = com.AutomatizacionService.config.FeignConfig.class)
public interface HorarioClient {

    @GetMapping("/todos")
    List<Map<String, Object>> listarTodos();

    @GetMapping("/public/cambios")
    Map<String, Object> verificarCambios(@RequestParam(required = false) String ultimaVersion);
    @GetMapping("/medico/{id}")
    List<Map<String, Object>> listarPorMedico(@PathVariable Long id);
}
