package com.authservice.controller.protect.endpointsProtect;

import com.authservice.client.CitaClient;
import com.authservice.client.MedicoClient;
import com.authservice.client.PacienteClient;
import com.authservice.client.UsuarioClient;
import org.springframework.context.annotation.Bean;
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

    public AdminController(UsuarioClient usuarioClient,
                           PacienteClient pacienteClient,
                           MedicoClient medicoClient,
                           CitaClient citaClient) {
        this.usuarioClient = usuarioClient;
        this.pacienteClient = pacienteClient;
        this.medicoClient = medicoClient;
        this.citaClient = citaClient;
    }

    @GetMapping("/usuarios")
    public List<Object> listarUsuarios() {
        return usuarioClient.listarUsuarios();
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