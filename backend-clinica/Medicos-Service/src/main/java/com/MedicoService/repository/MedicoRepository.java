package com.MedicoService.repository;

import com.MedicoService.model.Medico;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface MedicoRepository extends JpaRepository<Medico, Long> {
    Optional<Medico> findByUsuario(String usuario);
    @Query("SELECT m FROM Medico m WHERE LOWER(m.especialidad) LIKE LOWER(CONCAT('%', :especialidad, '%'))")
    List<Medico> findByEspecialidad(@Param("especialidad") String especialidad);

}