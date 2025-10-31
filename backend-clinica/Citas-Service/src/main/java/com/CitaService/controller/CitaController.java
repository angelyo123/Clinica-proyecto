package com.CitaService.controller;

import com.CitaService.model.Cita;
import com.CitaService.model.CitaDTO;
import com.CitaService.model.CitaMedicoDTO;
import com.CitaService.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cita")
//@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MEDICO','ROLE_PACIENTE')")
public class CitaController {

    @Autowired
    private CitaService citaService;

    @GetMapping("/listar")
    public List<Cita> listar() {
        return citaService.listar();
    }

    @GetMapping("/obtener/{id}")
    public Cita obtener(@PathVariable Long id) {
        return citaService.obtener(id);
    }

    @PostMapping("/crear")
    @PreAuthorize("permitAll()")
    public Cita crear(@RequestBody Cita cita) {
        return citaService.crear(cita);
    }

    @GetMapping("/listarPorPaciente")
    public List<Cita> listarPorPaciente(@RequestParam Long pacienteId) {
        return citaService.listarPorPaciente(pacienteId);
    }

    @GetMapping("/listarPorMedico")
    public ResponseEntity<List<Cita>> listarPorMedico(@RequestParam Long medicoId) {
        List<Cita> citas = citaService.listarPorMedico(medicoId);
        return ResponseEntity.ok(citas);
    }

    @PutMapping("/actualizarEstado/{id}")
    public CitaDTO actualizarEstado(@PathVariable Long id, @RequestParam String estado) {
        return citaService.actualizarEstado(id, estado);
    }

    @DeleteMapping("/eliminar/{id}")
    public void eliminar(@PathVariable Long id) {
        citaService.eliminar(id);
    }

    @GetMapping("/detalle/{id}")
    public ResponseEntity<CitaDTO> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtenerDetalle(id));
    }

    @GetMapping("/detallePorMedico")
    @PreAuthorize("hasAnyAuthority('ROLE_MEDICO','ROLE_ADMIN')")
    public ResponseEntity<List<CitaMedicoDTO>> listarDetallePorMedico(@RequestParam Long medicoId) {
        return ResponseEntity.ok(citaService.listarCitasPorMedicoConPacientes(medicoId));
    }

    @GetMapping("/listar/detalles")
    public List<CitaDTO> listarDetalles() {
        return citaService.listarDetalles();
    }

    @PostMapping("/crear/detalle")
    public ResponseEntity<CitaDTO> crearDetalle(@RequestBody CitaDTO citaDTO) {
        CitaDTO citaCreada = citaService.crearDetalle(citaDTO);
        return ResponseEntity.status(201).body(citaCreada); // 201 Created
    }

    @PutMapping("/actualizar/detalle/{id}")
    public ResponseEntity<CitaDTO> actualizarDetalle(@PathVariable Long id, @RequestBody CitaDTO citaDTO) {
        CitaDTO citaActualizada = citaService.actualizarDetalle(id, citaDTO);
        return ResponseEntity.ok(citaActualizada);
    }

    @GetMapping("/listarPorPaciente/detalles")
    public List<CitaDTO> listarDetallesPorPaciente(@RequestParam Long pacienteId) {

        return citaService.listarDetallesPorPaciente(pacienteId);
    }

    @GetMapping("/listarPorMedico/detalles")
    @PreAuthorize("hasAnyAuthority('ROLE_MEDICO', 'ROLE_ADMIN')")
    public List<CitaDTO> listarDetallesPorMedico(@RequestParam Long medicoId) {

        return citaService.listarDetallesPorMedico(medicoId);
    }

    @PutMapping("/cancelar/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PACIENTE','ROLE_MEDICO')")
    public ResponseEntity<CitaDTO> cancelarCita(@PathVariable Long id) {
        CitaDTO citaCancelada = citaService.actualizarEstado(id, "CANCELADA");
        return ResponseEntity.ok(citaCancelada);
    }

    @PutMapping("/cancelar/porPaciente/{pacienteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_PACIENTE','ROLE_ADMIN')")
    public ResponseEntity<String> cancelarCitasPorPaciente(@PathVariable Long pacienteId) {
        citaService.cancelarCitasPorPaciente(pacienteId);
        return ResponseEntity.ok("Todas las citas del paciente han sido canceladas.");
    }

}