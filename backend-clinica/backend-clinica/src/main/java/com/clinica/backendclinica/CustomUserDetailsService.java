package com.clinica.backendclinica;

import com.clinica.backendclinica.model.Rol;
import com.clinica.backendclinica.model.Usuario;
import com.clinica.backendclinica.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // Convertimos cada Rol a una autoridad entendible por Spring Security
        var authorities = usuario.getRoles().stream()
                .map(Rol::getNombre) // Ejemplo: "ROLE_ADMIN" o "ROLE_USER"
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),  // ya encriptado con BCrypt
                authorities
        );
    }
}
