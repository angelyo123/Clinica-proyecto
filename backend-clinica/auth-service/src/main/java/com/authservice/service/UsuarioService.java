package com.authservice.service;

import com.authservice.client.PacienteClient;
import com.authservice.model.PacienteDatosDTO;
import com.authservice.model.RegistroPacienteRequest;
import com.authservice.model.Rol;
import com.authservice.model.Usuario;
import com.authservice.repository.RolRepository;
import com.authservice.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PacienteClient pacienteClient;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder, PacienteClient pacienteClient) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.pacienteClient = pacienteClient;
    }
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario registrarPaciente(RegistroPacienteRequest request) {

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());

        usuario.setPassword(passwordEncoder.encode(request.getPassword()));

        Rol rol = rolRepository.findByNombre("ROLE_PACIENTE")
                .orElseThrow(() -> new RuntimeException("Rol de paciente no encontrado."));
        usuario.getRoles().add(rol);

        Usuario nuevoUsuario = usuarioRepository.save(usuario);

        PacienteDatosDTO datosPaciente = new PacienteDatosDTO(
                request.getNombre(),
                request.getDni(),
                request.getTelefono(),
                request.getUsername()
        );

        try {
            pacienteClient.crearPaciente(datosPaciente);

        } catch (Exception e) {
            System.err.println("Advertencia: El usuario se creó, pero falló el registro de datos personales en el Paciente-Service: " + e.getMessage());
        }

        return nuevoUsuario;
    }

    public Usuario registrarMedico(Usuario usuario) {
        Rol rolMedico = rolRepository.findByNombre("ROLE_MEDICO")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_MEDICO no encontrado"));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRoles(Collections.singleton(rolMedico));
        return usuarioRepository.save(usuario);
    }

    public Usuario registrarAdmin(Usuario usuario) {
        Rol rolAdmin = rolRepository.findByNombre("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_ADMIN no encontrado"));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRoles(Collections.singleton(rolAdmin));
        return usuarioRepository.save(usuario);
    }

}