package com.clinica.backendclinica.controller;

import com.clinica.backendclinica.model.Cita;
import com.clinica.backendclinica.model.Paciente;
import com.clinica.backendclinica.model.Usuario;
import com.clinica.backendclinica.repository.PacienteRepository;
import com.clinica.backendclinica.repository.UsuarioRepository;
import com.clinica.backendclinica.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/citas") // ✅ RUTA UNIFICADA
public class CitaController {

    @Autowired
    private CitaService citaService;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','PACIENTE')")
    public ResponseEntity<Cita> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.buscarCita(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO')")
    public ResponseEntity<Cita> actualizar(@PathVariable Long id, @RequestBody Cita cita) {
        return ResponseEntity.ok(citaService.actualizarCita(cita, id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        citaService.eliminarCita(id);
        return ResponseEntity.noContent().build();
    }

    // 📌 Paciente reserva su propia cita
    @PreAuthorize("hasAnyRole('PACIENTE','ADMIN')")
    @PostMapping
    public ResponseEntity<Cita> reservarCita(@Valid @RequestBody Cita cita, Authentication auth) {
        String username = auth.getName();

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Paciente paciente = pacienteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        cita.setPaciente(paciente);

        return ResponseEntity.ok(citaService.crearCita(cita));
    }

    // 📌 Paciente ve sus citas, admin y médico ven todas
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','PACIENTE')")
    @GetMapping
    public ResponseEntity<List<Cita>> listarCitas(Authentication auth) {
        return ResponseEntity.ok(citaService.listarCitas(auth));
    }
}
