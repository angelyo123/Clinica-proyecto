package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.PlantillaCampo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantillaCampoRepository extends JpaRepository<PlantillaCampo, Long> {

    void deleteBySeccionId(Long seccionId);

    List<PlantillaCampo> findBySeccionId(Long seccionId);

    List<PlantillaCampo> findBySeccion_Plantilla_Id(Long plantillaId);

}