package com.clinica.backendclinica.repository;

import com.clinica.backendclinica.model.Cita;
import com.clinica.backendclinica.model.Medico;
import com.clinica.backendclinica.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    boolean existsByPacienteAndFechaHora(Paciente paciente, LocalDateTime fechaHora);
    List<Cita> findByPaciente(Paciente paciente);
    List<Cita> findByMedico(Medico medico);
}