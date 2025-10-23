package com.AutomatizacionService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
@FeignClient(
        name = "paciente-service",
        url = "http://localhost:8082/paciente",
        configuration = com.AutomatizacionService.config.FeignConfig.class
)
public interface PacienteClient {
    // 🔹 Obtener los datos públicos de un paciente (sin necesidad de token)
    @GetMapping("/public/obtener/{id}")
    Map<String, Object> obtenerPacientePublico(@PathVariable Long id);
}
