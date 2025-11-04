package com.AutomatizacionService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "medico-service", url = "http://localhost:8083/medico",
        configuration = com.AutomatizacionService.config.FeignConfig.class)

public interface MedicoClient {
    @GetMapping("/public/listar")
    List<Map<String, Object>> listarMedicos();

    @GetMapping("/public/cambios")
    Map<String, Object> verificarCambios(@RequestParam(required = false) String ultimaVersion);

}
