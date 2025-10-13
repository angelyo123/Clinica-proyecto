package com.clinica.backendclinica.repository;

import com.clinica.backendclinica.model.Medico;
import com.clinica.backendclinica.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico,Long> {
    Optional<Medico> findByUsuario(Usuario usuario); // 👈 Nuevo método
}
