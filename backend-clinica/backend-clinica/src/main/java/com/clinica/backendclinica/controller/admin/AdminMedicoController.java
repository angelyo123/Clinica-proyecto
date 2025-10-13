package com.clinica.backendclinica.controller.admin;

import com.clinica.backendclinica.model.Medico;
import com.clinica.backendclinica.service.MedicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/medico")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminMedicoController {

    @Autowired
    private MedicoService medicoService;

    @GetMapping("/listar")
    public List<Medico> listar() {
        return medicoService.listar();
    }

    @GetMapping("/obtener/{id}")
    public Medico obtener(@PathVariable Long id) {
        return medicoService.ObtenerPorId(id);
    }

    @PostMapping("/crear")
    public Medico crear(@RequestBody Medico medico) {
        return medicoService.Crear(medico);
    }

    @PutMapping("/actualizar/{id}")
    public Medico actualizar(@PathVariable Long id, @RequestBody Medico medico) {
        return medicoService.Actualizar(id, medico);
    }

    @DeleteMapping("/eliminar/{id}")
    public void eliminar(@PathVariable Long id) {
        medicoService.Eliminar(id);
    }
}