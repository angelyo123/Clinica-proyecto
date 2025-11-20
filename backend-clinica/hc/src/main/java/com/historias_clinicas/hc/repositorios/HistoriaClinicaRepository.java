package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {}