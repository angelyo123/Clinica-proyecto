package com.clinica.backendclinica.service;

import com.clinica.backendclinica.model.Rol;
import com.clinica.backendclinica.model.Usuario;
import com.clinica.backendclinica.repository.RolRepository;
import com.clinica.backendclinica.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository  usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @PostConstruct
    public void verificarPassword() {
        String hash = "$2a$12$6QY/IihgMbMTStnT7gbGEuegNjaRW8pkcDXnrg43D.9o0Ou4StgGi";
        boolean coincide = passwordEncoder.matches("1234", hash);
        System.out.println("🔐 ¿Coincide '1234' con el hash? => " + coincide);
    }

    @Autowired
    private RolRepository rolRepository;

    @Override
    public List<Usuario> BuscarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario BuscarPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Override
    public Usuario Crear(Usuario usuario) {
        System.out.println("🟢 Password recibido antes de encriptar: '" + usuario.getPassword() + "'");

        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new RuntimeException("El usuario ya existe");
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }


    @Override
    public Usuario Actualizar(Long id, Usuario usuario) {
        Usuario existe= usuarioRepository.findById(id).orElse(null);
        if(existe!=null)
        {
            existe.setUsername(usuario.getUsername());
            existe.setPassword(usuario.getPassword());
            existe.setRoles(usuario.getRoles());
            return  usuarioRepository.save(existe);
        }
        return null;
    }

    @Override
    public void Eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    public Usuario asignarRol(Usuario usuario, String nombreRol) {
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));
        usuario.setRoles(new HashSet<>(List.of(rol)));

        return usuario;
    }


}
