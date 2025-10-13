package com.clinica.backendclinica.repository;

import com.clinica.backendclinica.model.Paciente;
import com.clinica.backendclinica.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente,Long> {
    Optional<Paciente> findByUsuario(Usuario usuario);

    Usuario usuario(Usuario usuario);
}
