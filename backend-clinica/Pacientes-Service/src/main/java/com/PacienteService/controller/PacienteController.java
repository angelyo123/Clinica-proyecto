package com.PacienteService.controller;

import com.PacienteService.client.AutomatizacionClient;
import com.PacienteService.client.CitaClient;
import com.PacienteService.client.MedicoClient;
import com.PacienteService.model.Paciente;

import com.PacienteService.model.PacienteBasicoDTO;
import com.PacienteService.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/paciente")
//@PreAuthorize("hasAnyAuthority('ROLE_PACIENTE', 'ROLE_ADMIN', 'ROLE_MEDICO')")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private MedicoClient medicoClient;

    @Autowired
    private CitaClient citaClient;
    @Autowired
    private AutomatizacionClient automatizacionClient;

    // ✅ CRUD básico de pacientes
    @GetMapping("/listar")
    public List<Paciente> listar() {
        return pacienteService.listar();
    }

    @PostMapping("/crear")
    public Paciente crear(@RequestBody Paciente paciente) {
        return pacienteService.Crear(paciente);
    }

    @GetMapping("/obtener/{id}")
    public Paciente obtener(@PathVariable Long id) {
        return pacienteService.ObtenerId(id);
    }

    // ✅ Comunicación con otros microservicios
    @GetMapping("/medicos")
    public List<Map<String, Object>> listarMedicos() {
        return medicoClient.listar();
    }

    @GetMapping("/citas")
    public List<Map<String, Object>> listarCitasPorPaciente(@RequestParam Long pacienteId) {
        return citaClient.listarPorPaciente(pacienteId);
    }

    @PostMapping("/citas")
    public Map<String, Object> crearCita(@RequestBody Map<String, Object> cita) {
        return citaClient.crearCita(cita);
    }

    @GetMapping("/perfil")
    public ResponseEntity<?> perfilPaciente(Authentication auth) {
        String username = auth.getName();
        Paciente paciente = pacienteService.obtenerPorUsuario(username);
        return ResponseEntity.ok(paciente);
    }

    @GetMapping("/public/obtener/{id}")
    @PreAuthorize("permitAll()")
    public PacienteBasicoDTO obtenerPublico(@PathVariable Long id) {
        Paciente p = pacienteService.ObtenerId(id);
        return new PacienteBasicoDTO(p.getId(), p.getNombre(), p.getTelefono());
    }

    @PostMapping("/ia/solicitud")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<?> solicitarIA(@RequestBody Map<String, String> body) {
        String mensaje = body.get("mensaje");
        Long pacienteId = Long.valueOf(body.get("pacienteId"));

        Map<String, Object> solicitud = Map.of(
                "pacienteId", pacienteId,
                "mensaje", mensaje
        );

        // 🔹 Llamamos al microservicio de Automatización
        Map<String, Object> respuesta = automatizacionClient.enviarSolicitudIA(solicitud);

        return ResponseEntity.ok(respuesta);
    }

}