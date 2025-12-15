package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.PlantillaAccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantillaAccionRepo
        extends JpaRepository<PlantillaAccion, Long> {

    List<PlantillaAccion> findByPlantillaId(Long plantillaId);

    boolean existsByPlantillaId(Long plantillaId);
    void deleteByPlantillaId(Long plantillaId);
}