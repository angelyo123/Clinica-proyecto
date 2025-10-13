package com.PacienteService.repository;

import com.PacienteService.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    // Si en tu entidad Paciente el campo 'usuario' es un String (por ejemplo, username)
    Optional<Paciente> findByUsuario(String usuario);
}