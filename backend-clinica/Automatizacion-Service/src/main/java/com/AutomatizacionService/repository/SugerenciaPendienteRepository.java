package com.AutomatizacionService.repository;

import com.AutomatizacionService.model.SugerenciaPendiente;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SugerenciaPendienteRepository extends JpaRepository<SugerenciaPendiente, Long> {

    List<SugerenciaPendiente> findByConfirmadaFalse();

    // ✅ Buscar la última sugerencia pendiente de un paciente
    Optional<SugerenciaPendiente> findTopByPacienteIdAndConfirmadaFalseOrderByIdDesc(Long pacienteId);

    @Transactional
    @Modifying
    @Query("UPDATE SugerenciaPendiente s SET s.confirmada = true WHERE s.pacienteId = :pacienteId")
    void marcarComoConfirmada(Long pacienteId);
}