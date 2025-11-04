package com.AutomatizacionService.repository;

import com.AutomatizacionService.model.entity.ConversacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversacionRepository extends JpaRepository<ConversacionEntity, Long> {
    List<ConversacionEntity> findByPacienteIdOrderByFechaAsc(Long pacienteId);
}