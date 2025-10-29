package com.CitaService.repository;

import com.CitaService.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByIdPaciente(Long idPaciente);
    List<Cita> findByIdMedico(Long idMedico);
    void deleteByIdPaciente(Long idPaciente);
}