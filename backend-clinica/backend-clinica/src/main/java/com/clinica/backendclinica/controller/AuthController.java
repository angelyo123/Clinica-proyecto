package com.clinica.backendclinica.controller;

import com.clinica.backendclinica.JwtUtil;
import com.clinica.backendclinica.model.Medico;
import com.clinica.backendclinica.model.Paciente;
import com.clinica.backendclinica.model.Usuario;
import com.clinica.backendclinica.model.login.JwtResponse;
import com.clinica.backendclinica.model.login.LoginRequest;
import com.clinica.backendclinica.service.MedicoService;
import com.clinica.backendclinica.service.PacienteService;
import com.clinica.backendclinica.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;
    private final MedicoService medicoService;
    private final PacienteService pacienteService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UsuarioService usuarioService, MedicoService medicoService, PacienteService pacienteService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
        this.medicoService = medicoService;
        this.pacienteService = pacienteService;
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        String token = jwtUtil.generateToken(authentication);
        return ResponseEntity.ok(new JwtResponse(token));
    }



    @PostMapping("/register/paciente")
    public ResponseEntity<Map<String, Object>> registerPaciente(@RequestBody Paciente paciente) {
        Map<String, Object> response = new HashMap<>();
        try {
            Usuario usuario = paciente.getUsuario();
            usuario = usuarioService.asignarRol(usuario, "ROLE_PACIENTE");
            usuario = usuarioService.Crear(usuario); // 🔹 Cifrado y guardado aquí

            paciente.setUsuario(usuario);
            pacienteService.guardarPaciente(paciente); // 👈 solo guarda paciente

            response.put("message", "Paciente registrado correctamente");
            response.put("status", 200);
            response.put("username", usuario.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Error al registrar paciente: " + e.getMessage());
            response.put("status", 400);
            return ResponseEntity.badRequest().body(response);
        }
    }


    @PostMapping("/register/medico")
    public ResponseEntity<?> registerMedico(@RequestBody Medico medico) {
        Usuario usuario = medico.getUsuario();
        usuarioService.asignarRol(usuario, "ROLE_MEDICO"); // método nuevo
        Usuario nuevoUsuario = usuarioService.Crear(usuario);

        medico.setUsuario(nuevoUsuario);
        medicoService.Crear(medico);

        return ResponseEntity.ok("Médico registrado correctamente");
    }

}
