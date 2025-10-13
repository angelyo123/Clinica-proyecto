package com.clinica.backendclinica.controller;

import com.clinica.backendclinica.model.Cita;
import com.clinica.backendclinica.model.Medico;
import com.clinica.backendclinica.service.CitaService;
import com.clinica.backendclinica.service.MedicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medico")
@PreAuthorize("hasAuthority('ROLE_MEDICO')")
public class MedicoController {

    @Autowired
    private CitaService citaService;

    @GetMapping("/citas")
    public List<Cita> listarCitasDelMedico(@RequestParam Long medicoId) {
        return citaService.listarPorMedico(medicoId);
    }
}