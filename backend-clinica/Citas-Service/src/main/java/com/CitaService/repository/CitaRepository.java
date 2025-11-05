package com.CitaService.repository;

import com.CitaService.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByIdPaciente(Long idPaciente);

    List<Cita> findByIdMedico(Long idMedico);

    // 🧠 Compatibilidad hacia atrás (por si se usa en otros puntos)
    boolean existsByIdHorarioAndEstadoIn(Long idHorario, List<String> estados);

    // ✅ Nueva validación: horario + fecha específica + estado
    boolean existsByIdHorarioAndFechaCitaAndEstadoIn(Long idHorario, LocalDate fechaCita, List<String> estados);
}
