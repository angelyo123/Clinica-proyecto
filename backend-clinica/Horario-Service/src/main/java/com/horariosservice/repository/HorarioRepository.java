package com.horariosservice.repository;

import com.horariosservice.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;


public interface HorarioRepository extends JpaRepository<Horario, Long> {
    List<Horario> findByMedicoId(Long medicoId);
    boolean existsByMedicoIdAndDiaSemanaAndHoraInicio(Long medicoId, String diaSemana, LocalTime horaInicio);
    // 🧨 (Opcional) Eliminar todos los horarios de un médico específico
    void deleteByMedicoId(Long medicoId);
}
