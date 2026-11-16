package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.PlantillaSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlantillaSnapshotRepo extends JpaRepository<PlantillaSnapshot, Long> {

    Optional<PlantillaSnapshot> findTopByPlantillaIdOrderByIdDesc(Long plantillaId);
}