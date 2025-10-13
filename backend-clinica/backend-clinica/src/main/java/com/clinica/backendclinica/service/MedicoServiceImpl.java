package com.clinica.backendclinica.service;

import com.clinica.backendclinica.model.Medico;
import com.clinica.backendclinica.model.Rol;
import com.clinica.backendclinica.model.Usuario;
import com.clinica.backendclinica.repository.MedicoRepository;
import com.clinica.backendclinica.repository.RolRepository;
import com.clinica.backendclinica.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MedicoServiceImpl implements MedicoService {
    @Autowired
    MedicoRepository medicoRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<Medico> listar() {
        return medicoRepository.findAll();
    }

    @Override
    public Medico ObtenerPorId(Long id) {
        return  medicoRepository.findById(id).orElse(null);
    }

    @Override
    public Medico Crear(Medico medico) {
        Usuario usuario = medico.getUsuario();

        if (usuario == null) {
            usuario = new Usuario();
            usuario.setUsername(medico.getNombre());
            usuario.setPassword("123456");
        }

        Rol rolMedico = rolRepository.findByNombre("ROLE_MEDICO")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_MEDICO no encontrado"));
        usuario.setRoles(new HashSet<>(List.of(rolMedico)));


        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);

        medico.setUsuario(usuario);
        return medicoRepository.save(medico);
    }

    @Override
    public Medico Actualizar(Long id, Medico medico) {
        Medico existe=  medicoRepository.findById(id).orElse(null);
        if(existe!=null)
        {
            existe.setNombre(medico.getNombre());
            existe.setEspecialidad(medico.getEspecialidad());
            existe.setTelefono(medico.getTelefono());
            return medicoRepository.save(existe);
        }
        return null;
    }

    @Override
    public void Eliminar(Long id) {
        medicoRepository.deleteById(id);
    }
}
