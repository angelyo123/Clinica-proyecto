package com.AutomatizacionService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/public/medico/{id}")
    Map<String, Object> listarPublicoPorMedico(@PathVariable Long id);

    // 🔹 Crear horario
    @PostMapping("/crear")
    Map<String, Object> crearHorario(@RequestBody Map<String, Object> horario);

    // 🔹 Actualizar horario existente
    @PutMapping("/{id}")
    Map<String, Object> actualizarHorario(@PathVariable("id") Long id, @RequestBody Map<String, Object> horario);

    // 🔹 Eliminar horario por ID
    @DeleteMapping("/{id}")
    Map<String, Object> eliminarHorario(@PathVariable("id") Long id);

    // 🔹 Eliminar todos los horarios de un médico
    @DeleteMapping("/eliminar/porMedico/{medicoId}")
    Map<String, Object> eliminarPorMedico(@PathVariable("medicoId") Long medicoId);

    // 🔹 Actualizar disponibilidad (true / false)
    @PutMapping("/{id}/disponibilidad/{disponible}")
    Map<String, Object> actualizarDisponibilidad(
            @PathVariable("id") Long id,
            @PathVariable("disponible") Boolean disponible
    );
}
