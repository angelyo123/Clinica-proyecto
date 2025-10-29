package com.AutomatizacionService.repository;

import com.AutomatizacionService.model.SugerenciaPendiente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SugerenciaPendienteRepository extends JpaRepository<SugerenciaPendiente, Long> {
    List<SugerenciaPendiente> findByConfirmadaFalse();
}