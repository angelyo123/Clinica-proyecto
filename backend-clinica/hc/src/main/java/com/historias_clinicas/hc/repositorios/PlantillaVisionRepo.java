package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.PlantillaVision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlantillaVisionRepo
        extends JpaRepository<PlantillaVision, Long> {

    Optional<PlantillaVision> findByPlantillaId(Long plantillaId);
}
