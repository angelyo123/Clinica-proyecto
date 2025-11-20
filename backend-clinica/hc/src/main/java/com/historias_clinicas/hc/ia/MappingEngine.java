package com.historias_clinicas.hc.ia;

import com.historias_clinicas.hc.entidades.Plantilla;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.repositorios.PlantillaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MappingEngine {

    private final PlantillaCampoRepository campoRepo;

    public Map<PlantillaCampo, String> mapearDatosAPlantilla(
            Map<String, String> datosIA,
            Long plantillaId
    ) {
        List<PlantillaCampo> campos = campoRepo.findBySeccion_Plantilla_Id(plantillaId);

        Map<PlantillaCampo, String> result = new LinkedHashMap<>();

        for (var entry : datosIA.entrySet()) {
            String clave = entry.getKey().toLowerCase();
            String valor = entry.getValue() == null ? "" : entry.getValue().toString();

            if (valor.isBlank()) continue;

            campos.stream()
                    .filter(c -> c.getNombreCampo().equalsIgnoreCase(clave))
                    .findFirst()
                    .ifPresent(c -> result.put(c, valor));
        }

        return result;
    }

    public Map<String, String> aPlano(Map<String, Object> datos) {
        Map<String, String> plano = new LinkedHashMap<>();

        for (var entry : datos.entrySet()) {
            plano.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
        }

        return plano;
    }

}