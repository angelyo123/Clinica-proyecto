package com.PacienteService.client;

import com.PacienteService.model.CitaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "citas-service", url = "http://localhost:8084/cita")
public interface CitaClient {

    @GetMapping("/listarPorPaciente/detalles")
    List<CitaDTO> listarDetallesPorPaciente(@RequestParam Long pacienteId);

    @PostMapping("/crear")
    Map<String, Object> crearCita(@RequestBody Map<String, Object> cita);

    @GetMapping("/detalle/{id}")
    Map<String, Object> obtenerDetalle(@PathVariable Long id);

    @DeleteMapping("/eliminar/{id}")
    void eliminar(@PathVariable Long id);

    @DeleteMapping("/paciente/{idPaciente}")
    void eliminarPorPaciente(@PathVariable("idPaciente") Long idPaciente);
}
