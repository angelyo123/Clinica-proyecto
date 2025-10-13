package com.PacienteService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "citas-service", url = "http://localhost:8084/cita")
public interface CitaClient {

    @GetMapping("/listarPorPaciente")
    List<Map<String, Object>> listarPorPaciente(@RequestParam Long pacienteId);

    @PostMapping("/crear")
    Map<String, Object> crearCita(@RequestBody Map<String, Object> cita);

    @GetMapping("/detalle/{id}")
    Map<String, Object> obtenerDetalle(@PathVariable Long id);

}
