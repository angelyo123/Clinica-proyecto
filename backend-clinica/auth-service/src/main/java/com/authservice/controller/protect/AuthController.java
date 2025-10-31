package com.authservice.controller.protect;

import com.authservice.model.RegistroPacienteRequest;
import com.authservice.model.Usuario;
import com.authservice.repository.UsuarioRepository;
import com.authservice.security.JwtUtil;
import com.authservice.model.login.JwtResponse;
import com.authservice.model.login.LoginRequest;
import com.authservice.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtUtil.generateToken(authentication, usuario.getId());

        return ResponseEntity.ok(new JwtResponse(token));
    }

    // ✅ REGISTRO PACIENTE
    @PostMapping("/register/paciente")
    public ResponseEntity<Map<String, Object>> registerPaciente(@RequestBody RegistroPacienteRequest request) {

        Usuario nuevo = usuarioService.registrarPaciente(request);

        Map<String, Object> response = new HashMap<>();
        response.put("usuarioId", nuevo.getId());
        response.put("username", nuevo.getUsername());

        return ResponseEntity.ok(response);
    }

    // ✅ REGISTRO MÉDICO
    @PostMapping("/register/medico")
    public ResponseEntity<Map<String, Object>> registerMedico(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(password);

        Usuario nuevo = usuarioService.registrarMedico(usuario);

        Map<String, Object> response = new HashMap<>();
        response.put("usuarioId", nuevo.getId());
        response.put("username", nuevo.getUsername());

        return ResponseEntity.ok(response);
    }

    // ✅ REGISTRO ADMIN
    @PostMapping("/register/admin")
    public ResponseEntity<Map<String, Object>> registrarAdmin(@RequestBody Map<String, Object> request) {
        Usuario usuario = new Usuario();
        usuario.setUsername(request.get("username").toString());
        usuario.setPassword(request.get("password").toString());

        Usuario nuevo = usuarioService.registrarAdmin(usuario);

        Map<String, Object> response = new HashMap<>();
        response.put("id", nuevo.getId());
        response.put("username", nuevo.getUsername());
        response.put("roles", nuevo.getRoles());
        return ResponseEntity.ok(response);
    }
}
