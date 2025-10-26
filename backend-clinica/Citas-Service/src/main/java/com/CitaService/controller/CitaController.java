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
    public Cita crear(@RequestBody Cita cita) {
        return citaService.crear(cita);
    }

    @GetMapping("/listarPorPaciente")
    public List<Cita> listarPorPaciente(@RequestParam Long pacienteId) {
        return citaService.listarPorPaciente(pacienteId);
    }

    @GetMapping("/listarPorMedico")
    public List<Cita> listarPorMedico(@RequestParam Long medicoId) {
        return citaService.listarPorMedico(medicoId);
    }

    @PutMapping("/actualizarEstado/{id}")
    public Cita actualizarEstado(@PathVariable Long id, @RequestParam String estado) {
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

}