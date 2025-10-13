package com.clinica.backendclinica.controller.admin;

import com.clinica.backendclinica.model.Paciente;
import com.clinica.backendclinica.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/paciente")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPacienteController {

    @Autowired
    private PacienteService pacienteService;

    @GetMapping("/listar")
    public List<Paciente> listar() {
        return pacienteService.listar();
    }

    @GetMapping("/obtener/{id}")
    public Paciente buscar(@PathVariable Long id) {
        return pacienteService.ObtenerId(id);
    }

    @PostMapping("/crear")
    public Paciente crear(@RequestBody Paciente paciente) {
        return pacienteService.Crear(paciente);
    }

    @PutMapping("/actualizar/{id}")
    public Paciente actualizar(@PathVariable Long id, @RequestBody Paciente paciente) {
        return pacienteService.Actualizar(id, paciente);
    }

    @DeleteMapping("/eliminar/{id}")
    public void eliminar(@PathVariable Long id) {
        pacienteService.Eliminar(id);
    }
}