package com.clinica.backendclinica.service;

import com.clinica.backendclinica.model.Paciente;
import com.clinica.backendclinica.model.Rol;
import com.clinica.backendclinica.model.Usuario;
import com.clinica.backendclinica.repository.PacienteRepository;
import com.clinica.backendclinica.repository.RolRepository;
import com.clinica.backendclinica.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Transactional
@Service
public class PacienteServiceImpl implements PacienteService{
    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<Paciente> listar() {
        return pacienteRepository.findAll();
    }

    @Override
    public Paciente ObtenerId(Long id) {
        return pacienteRepository.findById(id).orElse(null);
    }
    @Override
    public Paciente Crear(Paciente paciente) {
        Usuario usuario = paciente.getUsuario();

        // Si el paciente no trae usuario, crea uno por defecto
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setUsername(paciente.getDni());
            usuario.setPassword("123456");
        }

        // Asignar ROLE_PACIENTE
        Rol rolPaciente = rolRepository.findByNombre("ROLE_PACIENTE")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_PACIENTE no encontrado"));
        usuario.setRoles(new HashSet<>(List.of(rolPaciente)));


        // Cifrar contraseña
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        // Guardar usuario y paciente
        usuarioRepository.save(usuario);
        paciente.setUsuario(usuario);
        return pacienteRepository.save(paciente);
    }

    @Override
    public Paciente guardarPaciente(Paciente paciente) {
        // 👇 Sin tocar el usuario
        return pacienteRepository.save(paciente);
    }


    @Override
    public Paciente Actualizar(Long id, Paciente paciente) {
        Paciente existente = pacienteRepository.findById(id).orElse(null);
        if (existente != null)
        {
            existente.setNombre(paciente.getNombre());
            existente.setTelefono(paciente.getTelefono());
            existente.setDni(paciente.getDni());
            return  pacienteRepository.save(existente);
        }

        return null;
    }

    @Override
    public void Eliminar(Long id) {

    }
}
