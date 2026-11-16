package com.historias_clinicas.hc.repositorios;

import com.historias_clinicas.hc.entidades.PlantillaCampo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.historias_clinicas.hc.entidades.PlantillaCampo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantillaCampoRepository extends JpaRepository<PlantillaCampo, Long> {

    List<PlantillaCampo> findByPlantillaId(Long plantillaId);

    void deleteByPlantillaId(Long plantillaId);
}