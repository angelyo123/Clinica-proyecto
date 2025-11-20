package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriaClinicaVersionRepository extends JpaRepository<HistoriaClinicaVersion, Long> {}