package com.MedicoService.controller;

import com.MedicoService.model.Medico;
import com.MedicoService.service.MedicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medico")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE')")
public class MedicoController {

    @Autowired
    private MedicoService medicoService;

    @GetMapping("/listar")
    public List<Medico> listar() {
        return medicoService.listar();
    }

    @GetMapping("/obtener/{id}")
    public Medico obtener(@PathVariable Long id) {
        return medicoService.obtenerPorId(id);
    }

    @PostMapping("/crear")
    @PreAuthorize("permitAll()")
    public Medico crear(@RequestBody Medico medico) {
        return medicoService.crear(medico);
    }

    @PutMapping("/actualizar/{id}")
    public Medico actualizar(@PathVariable Long id, @RequestBody Medico medico) {
        return medicoService.actualizar(id, medico);
    }

    @DeleteMapping("/eliminar/{id}")
    public void eliminar(@PathVariable Long id) {
        medicoService.eliminar(id);
    }

    // endpoint accesible sin autenticación (por ejemplo para listar médicos disponibles)
    @GetMapping("/public/listar")
    @PreAuthorize("permitAll()")
    public List<Medico> listarPublico() {
        return medicoService.listar();
    }


    @GetMapping("/perfil")
    public ResponseEntity<?> perfilMedico(Authentication auth) {
        String username = auth.getName();
        Medico medico= medicoService.obtenerPorUsuario(username);
        return ResponseEntity.ok(medico);
    }

}