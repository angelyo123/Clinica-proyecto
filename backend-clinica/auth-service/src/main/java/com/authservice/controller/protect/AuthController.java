package com.authservice.controller.protect;


import com.authservice.model.Rol;
import com.authservice.model.Usuario;
import com.authservice.repository.RolRepository;
import com.authservice.security.JwtUtil;
import com.authservice.model.login.JwtResponse;
import com.authservice.model.login.LoginRequest;
import com.authservice.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200") // para Angular
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        String token = jwtUtil.generateToken(authentication);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @PostMapping("/register/paciente")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> registerPaciente(@RequestBody Usuario usuario) {
        Usuario nuevo = usuarioService.registrarPaciente(usuario);
        return ResponseEntity.ok("Paciente registrado correctamente con ID: " + nuevo.getId());
    }

    // ✅ REGISTRO MÉDICO
    @PostMapping("/register/medico")
    public ResponseEntity<?> registerMedico(@RequestBody Usuario usuario) {
        Usuario nuevo = usuarioService.registrarMedico(usuario);
        return ResponseEntity.ok("Médico registrado correctamente con ID: " + nuevo.getId());
    }

}