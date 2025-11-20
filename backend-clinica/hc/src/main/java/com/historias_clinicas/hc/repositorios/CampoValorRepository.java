package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.CampoValor;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampoValorRepository extends JpaRepository<CampoValor, Long> {
    List<CampoValor> findByVersion(HistoriaClinicaVersion version);

    Optional<CampoValor> findByVersionIdAndCampoId(Long versionId, Long campoId);

}