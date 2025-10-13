package com.clinica.backendclinica.controller;

import com.clinica.backendclinica.model.Cita;
import com.clinica.backendclinica.model.Medico;
import com.clinica.backendclinica.model.Paciente;
import com.clinica.backendclinica.service.CitaService;
import com.clinica.backendclinica.service.MedicoService;
import com.clinica.backendclinica.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/paciente")
@PreAuthorize("hasAuthority('ROLE_PACIENTE')")
public class PacienteController {

    @Autowired
    private MedicoService medicoService;

    @Autowired
    private CitaService citaService;

    @GetMapping("/medicos")
    public List<Medico> listarMedicosParaPacientes() {
        return medicoService.listar();
    }

    @GetMapping("/citas")
    public List<Cita> listarCitasDelPaciente(@RequestParam Long pacienteId) {
        return citaService.listarPorPaciente(pacienteId);
    }

    @PostMapping("/citas")
    public Cita crearCita(@RequestBody Cita cita) {
        return citaService.crearCita(cita);
    }
}
