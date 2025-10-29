package com.authservice.service;

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

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario registrarPaciente(Usuario usuario) {
        Rol rolPaciente = rolRepository.findByNombre("ROLE_PACIENTE")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_PACIENTE no encontrado"));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRoles(Collections.singleton(rolPaciente));
        return usuarioRepository.save(usuario);
    }

    public Usuario registrarMedico(Usuario usuario) {
        Rol rolMedico = rolRepository.findByNombre("ROLE_MEDICO")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_MEDICO no encontrado"));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRoles(Collections.singleton(rolMedico));
        return usuarioRepository.save(usuario);
    }
}