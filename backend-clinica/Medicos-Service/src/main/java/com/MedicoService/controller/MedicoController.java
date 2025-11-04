package com.MedicoService.controller;

import com.MedicoService.model.Medico;
import com.MedicoService.service.MedicoChangeTracker;
import com.MedicoService.service.MedicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/medico")
//@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE')")
public class MedicoController {

    @Autowired
    private MedicoService medicoService;
    @Autowired private MedicoChangeTracker tracker;
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

    @GetMapping("/usuario/{username}")
    public ResponseEntity<Medico> obtenerPorUsuario(@PathVariable String username) {
        Medico medico = medicoService.obtenerPorUsuario(username);
        return ResponseEntity.ok(medico);
    }

    @GetMapping("/public/especialidad")
    public List<Medico> listarPorEspecialidad(@RequestParam String especialidad) {
        return medicoService.listarPorEspecialidad(especialidad);
    }

    @GetMapping("/public/cambios")
    public Map<String, Object> verificarCambios(@RequestParam(required = false) String ultimaVersion) {
        LocalDateTime actual = tracker.obtenerUltimaActualizacion();
        boolean hayCambio = true;

        if (ultimaVersion != null && !ultimaVersion.isBlank()) {
            try {
                LocalDateTime ultima = LocalDateTime.parse(ultimaVersion);
                hayCambio = actual.isAfter(ultima); // ✅ comparación real de fechas
            } catch (Exception e) {
                hayCambio = true; // si hay error al parsear, forzamos actualización
            }
        }

        System.out.println("🕓 Última en tracker: " + actual + " | Última conocida: " + ultimaVersion + " | Cambio=" + hayCambio);

        return Map.of(
                "cambio", hayCambio,
                "ultimaVersion", actual.toString()
        );
    }

}