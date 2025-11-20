package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.PlantillaSeccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantillaSeccionRepository extends JpaRepository<PlantillaSeccion, Long>
{
    List<PlantillaSeccion> findByPlantillaId(Long plantillaId);
}