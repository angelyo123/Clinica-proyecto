package com.historias_clinicas.hc.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.entidades.CampoValor;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import com.historias_clinicas.hc.repositorios.HistoriaClinicaVersionRepository;
import com.historias_clinicas.hc.repositorios.CampoValorRepository;

import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.servicios.PlantillaProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IaService {

    private final ImageInterpreter imageInterpreter;
    private final TextInterpreter textInterpreter;
    private final MappingEngine mappingEngine;

    private final HistoriaClinicaVersionRepository versionRepo;
    private final CampoValorRepository campoValorRepo;
    private final PlantillaCampoRepository campoRepo;
    private final PlantillaProcessorService plantillaProcessorService;

    private final DeepSeekClient deepSeek;  // <--- USO DIRECTO DE DEEPSEEK

    private final ObjectMapper mapper = new ObjectMapper();

    // -------------------------------
    // INTERPRETAR TEXTO
    // -------------------------------
    public Map<PlantillaCampo, String> interpretarTexto(Long versionId, String texto) throws Exception {

        var version = versionRepo.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

        Long plantillaId = version.getHistoriaClinica().getPlantilla().getId();

        // 🔥 Obtener lista de campos exactos detectados por POI
        List<String> camposPlantilla = campoRepo.findBySeccion_Plantilla_Id(plantillaId)
                .stream()
                .map(PlantillaCampo::getNombreCampo)
                .collect(Collectors.toList());

        // 🔥 Interpretar texto usando ESA LISTA
        Map<String, Object> jsonIA = textInterpreter.interpretarTexto(
                texto,
                camposPlantilla,
                plantillaProcessorService.getListaCeldasParaDeepSeek()
        );

        Map<String, String> jsonPlano = mappingEngine.aPlano(jsonIA);

        // 🔥 Mapear datos IA → Plantilla
        var mapeo = mappingEngine.mapearDatosAPlantilla(
                jsonPlano,
                plantillaId
        );

        guardarCampos(version, mapeo);

        return mapeo;
    }



    // -------------------------------
    // INTERPRETAR IMAGEN
    // -------------------------------
    public Map<PlantillaCampo, String> interpretarImagen(Long versionId, MultipartFile file) throws Exception {

        var version = versionRepo.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

        Long plantillaId = version.getHistoriaClinica().getPlantilla().getId();

        // Leer texto desde imagen con OCR/IA
        String texto = imageInterpreter.leerImagen(file);

        // 🔥 Obtener lista de campos exactos detectados por POI
        List<String> camposPlantilla = campoRepo.findBySeccion_Plantilla_Id(plantillaId)
                .stream()
                .map(PlantillaCampo::getNombreCampo)
                .collect(Collectors.toList());

        // 🔥 Interpretar texto de la imagen con los campos
        Map<String, Object> jsonIA = textInterpreter.interpretarTexto(
                texto,
                camposPlantilla,
                plantillaProcessorService.getListaCeldasParaDeepSeek()
        );

        Map<String, String> jsonPlano = mappingEngine.aPlano(jsonIA);

        var mapeo = mappingEngine.mapearDatosAPlantilla(
                jsonPlano,
                plantillaId
        );

        guardarCampos(version, mapeo);

        return mapeo;
    }


    private void guardarCampos(HistoriaClinicaVersion version,
                               Map<PlantillaCampo, String> mapeo) {

        mapeo.forEach((campo, valor) -> {
            CampoValor cv = CampoValor.builder()
                    .version(version)
                    .campo(campo)
                    .valor(valor)
                    .build();

            campoValorRepo.save(cv);
        });
    }
}