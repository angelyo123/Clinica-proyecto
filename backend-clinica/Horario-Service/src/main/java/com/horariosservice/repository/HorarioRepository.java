package com.horariosservice.repository;

import com.horariosservice.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface HorarioRepository extends JpaRepository<Horario, Long> {
    List<Horario> findByMedicoId(Long medicoId);
}
