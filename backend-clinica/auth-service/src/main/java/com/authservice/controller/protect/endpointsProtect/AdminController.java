package com.authservice.controller.protect.endpointsProtect;

import com.authservice.client.CitaClient;
import com.authservice.client.MedicoClient;
import com.authservice.client.PacienteClient;
import com.authservice.client.UsuarioClient;
import com.authservice.model.Usuario;
import com.authservice.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UsuarioClient usuarioClient;
    private final PacienteClient pacienteClient;
    private final MedicoClient medicoClient;
    private final CitaClient citaClient;
    private final UsuarioService usuarioService;

    public AdminController(UsuarioClient usuarioClient,
                           PacienteClient pacienteClient,
                           MedicoClient medicoClient,
                           CitaClient citaClient, UsuarioService usuarioService) {
        this.usuarioClient = usuarioClient;
        this.pacienteClient = pacienteClient;
        this.medicoClient = medicoClient;
        this.citaClient = citaClient;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        List<Usuario> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/pacientes")
    public List<Object> listarPacientes() {
        return pacienteClient.listarPacientes();
    }

    @GetMapping("/medicos")
    public List<Object> listarMedicos() {
        return medicoClient.listarMedicos();
    }

    @GetMapping("/citas")
    public List<Object> listarCitas() {
        return citaClient.listarCitas();
    }
}