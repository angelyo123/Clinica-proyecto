package com.PacienteService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "automatizacion-service",
        url = "http://localhost:8085/api/ia",
        configuration = com.PacienteService.config.FeignConfig.class
)
public interface AutomatizacionClient {
    @PostMapping("/procesar-mensaje")
    Map<String, Object> enviarSolicitudIA(@RequestBody Map<String, Object> solicitud);
}
