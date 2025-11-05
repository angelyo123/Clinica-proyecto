package com.CitaService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "horarios-service",
        url = "http://localhost:8085/horarios",
        configuration = com.CitaService.config.FeignConfig.class
)
public interface HorarioClient {

    @GetMapping("/disponibles/{medicoId}")
    List<Map<String, Object>> listarDisponibles(@PathVariable Long medicoId);

    @GetMapping("/medico/{id}")
    List<Map<String, Object>> listarPorMedico(@PathVariable Long id);

    @PutMapping("/{id}/disponibilidad/{estado}")
    void actualizarDisponibilidad(@PathVariable("id") Long id,
                                  @PathVariable("estado") boolean estado);
}