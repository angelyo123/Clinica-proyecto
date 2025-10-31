package com.CitaService.client;

import com.CitaService.config.FeignConfig;
import com.CitaService.model.PacienteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "pacientes-service", url = "http://localhost:8082", configuration = FeignConfig.class)
public interface PacienteClient {
    @GetMapping("/paciente/{id}")
    Map<String, Object> findById(@PathVariable("id") Long id);

    @GetMapping("/paciente/public/obtener/{id}")
    PacienteDTO obtener(@PathVariable Long id);

}