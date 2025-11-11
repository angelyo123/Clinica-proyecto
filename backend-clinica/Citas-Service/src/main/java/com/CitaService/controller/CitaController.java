package com.CitaService.controller;

import com.CitaService.model.Cita;
import com.CitaService.model.CitaDTO;
import com.CitaService.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cita")
@CrossOrigin(origins = "http://localhost:4200")
public class CitaController {

    @Autowired
    private CitaService citaService;

    // ✅ Crear cita (DTO enriquecido)
    @PostMapping("/crear")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> crear(@RequestBody CitaDTO citaDTO) {
        try {
            CitaDTO citaCreada = citaService.crear(citaDTO);
            return ResponseEntity.status(201).body(citaCreada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Ocurrió un error inesperado al crear la cita",
                    "detalle", e.getMessage()
            ));
        }
    }

    // 🔹 Listar citas simples (sin detalles)
    @GetMapping("/listar")
    public List<Cita> listar() {
        return citaService.listar();
    }

    // 🔹 Obtener una cita simple
    @GetMapping("/obtener/{id}")
    public Cita obtener(@PathVariable Long id) {
        return citaService.obtener(id);
    }

    // 🔹 Listar citas simples por paciente
    @GetMapping("/listarPorPaciente")
    public List<Cita> listarPorPaciente(@RequestParam Long pacienteId) {
        return citaService.listarPorPaciente(pacienteId);
    }

    // 🔹 Listar citas simples por médico
    @GetMapping("/listarPorMedico")
    @PreAuthorize("permitAll()")
    public List<CitaDTO> listarPorMedico(@RequestParam Long medicoId) {
        return citaService.listarDetallesPorMedico(medicoId);
    }

    // 🔹 Obtener cita con detalles (DTO completo)
    @GetMapping("/detalle/{id}")
    public ResponseEntity<CitaDTO> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtenerDetalle(id));
    }

    // 🔹 Listar todas las citas detalladas
    @GetMapping("/listar/detalles")
    public List<CitaDTO> listarDetalles() {
        return citaService.listarDetalles();
    }

    // 🔹 Listar citas detalladas por paciente
    @GetMapping("/listarPorPaciente/detalles")
    public List<CitaDTO> listarDetallesPorPaciente(@RequestParam Long pacienteId) {
        return citaService.listarDetallesPorPaciente(pacienteId);
    }

    // 🔹 Listar citas detalladas por médico
    @GetMapping("/listarPorMedico/detalles")
    public List<CitaDTO> listarDetallesPorMedico(@RequestParam Long medicoId) {
        return citaService.listarDetallesPorMedico(medicoId);
    }

    // 🔹 Actualizar estado de una cita (confirmar, cancelar, etc.)
    @PutMapping("/actualizarEstado/{id}")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        try {
            String nuevoEstado = body.get("nuevoEstado");
            CitaDTO actualizada = citaService.actualizarEstado(id, nuevoEstado);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error al actualizar el estado de la cita",
                    "detalle", e.getMessage()
            ));
        }
    }

    // 🔹 Cancelar todas las citas de un paciente
    @PutMapping("/cancelar/porPaciente/{pacienteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_PACIENTE','ROLE_ADMIN')")
    public ResponseEntity<String> cancelarCitasPorPaciente(@PathVariable Long pacienteId) {
        citaService.cancelarCitasPorPaciente(pacienteId);
        return ResponseEntity.ok("Todas las citas del paciente han sido canceladas.");
    }

    // 🔹 Eliminar una cita
    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        citaService.eliminar(id);
        return ResponseEntity.ok("Cita eliminada correctamente.");
    }

    @PutMapping("/actualizar/detalle/{id}")
    //@PreAuthorize("hasAnyAuthority('ROLE_PACIENTE', 'ROLE_ADMIN', 'ROLE_MEDICO')")
    public ResponseEntity<?> actualizarDetalle(@PathVariable Long id, @RequestBody CitaDTO citaDTO) {
        try {
            CitaDTO actualizada = citaService.actualizarDetalle(id, citaDTO);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error al actualizar la cita",
                    "detalle", e.getMessage()
            ));
        }
    }
}
