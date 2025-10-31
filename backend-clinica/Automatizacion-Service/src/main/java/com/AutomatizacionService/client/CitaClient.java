package com.AutomatizacionService.client;

import com.AutomatizacionService.model.CitaRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(
        name = "cita-service",
        url = "http://localhost:8084/cita",
        configuration = com.AutomatizacionService.config.FeignConfig.class
)
public interface CitaClient {
    @PostMapping("/crear")
    Map<String, Object> crearCita(@RequestBody CitaRequest cita);

    @PutMapping("/cancelar/{id}")
    Map<String, Object> cancelarCita(@PathVariable("id") Long id);

    @PutMapping("/cancelar/porPaciente/{pacienteId}")
    Map<String, Object> cancelarPorPaciente(@PathVariable("pacienteId") Long pacienteId);
}
