package com.historias_clinicas.hc.ia;

import com.historias_clinicas.hc.entidades.Plantilla;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.repositorios.PlantillaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;

@Slf4j
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

            log.info("📥 Recibido desde IA: clave='{}' valor='{}'", clave, valor);

            String claveNorm = normalizar(clave);

            campos.stream()
                    .filter(c -> normalizar(c.getNombreCampo()).contains(claveNorm))
                    .findFirst()
                    .ifPresent(c -> {
                        log.info("🔗 MATCH → IA '{}' → PlantillaCampo '{}'", clave, c.getNombreCampo());
                        result.put(c, valor);
                    });

        }

        return result;
    }

    private String normalizar(String s) {
        if (s == null) return "";
        s = s.toLowerCase();
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return s.replaceAll("[^a-z0-9]", "");
    }


    public Map<String, String> aPlano(Map<String, Object> datos) {
        Map<String, String> plano = new LinkedHashMap<>();

        for (var entry : datos.entrySet()) {
            plano.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
        }


        return plano;
    }

}