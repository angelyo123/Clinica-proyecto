package com.AutomatizacionService.repository;

import com.AutomatizacionService.model.entity.medico.RegistroIAMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroIAMedicoRepository extends JpaRepository<RegistroIAMedico, Long> {
    List<RegistroIAMedico> findByMedicoIdOrderByFechaAsc(Long medicoId);
}