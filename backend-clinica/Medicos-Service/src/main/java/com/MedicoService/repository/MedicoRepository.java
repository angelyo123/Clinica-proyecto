package com.MedicoService.repository;

import com.MedicoService.model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface MedicoRepository extends JpaRepository<Medico, Long> {
    Optional<Medico> findByUsuario(String usuario);
}