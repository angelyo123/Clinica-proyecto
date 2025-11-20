package com.historias_clinicas.hc.servicios.impl;

import com.historias_clinicas.hc.entidades.*;
import com.historias_clinicas.hc.repositorios.*;
import com.historias_clinicas.hc.servicios.HistoriaClinicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {

    private final HistoriaClinicaRepository hcRepo;
    private final HistoriaClinicaVersionRepository versionRepo;
    private final CampoValorRepository valoresRepo;
    private final DocumentoAdjuntoRepository adjuntoRepo;
    private final PlantillaRepository plantillaRepo;

    @Override
    public HistoriaClinica crearHC(Long pacienteId, Long medicoId, Long plantillaId) {
        var plantilla = plantillaRepo.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        HistoriaClinica hc = HistoriaClinica.builder()
                .pacienteId(pacienteId)
                .medicoId(medicoId)
                .plantilla(plantilla)
                .estado("BORRADOR")
                .build();

        return hcRepo.save(hc);
    }

    @Override
    public HistoriaClinicaVersion crearVersion(Long hcId, String descripcion) {
        var hc = hcRepo.findById(hcId)
                .orElseThrow(() -> new RuntimeException("HC no encontrada"));

        int nuevaVersion = hc.getVersiones() == null ? 1 : hc.getVersiones().size() + 1;

        HistoriaClinicaVersion v = HistoriaClinicaVersion.builder()
                .historiaClinica(hc)
                .numeroVersion(nuevaVersion)
                .descripcion(descripcion)
                .fechaCreacion(java.time.LocalDateTime.now())
                .build();

        return versionRepo.save(v);
    }

    @Override
    public CampoValor guardarCampo(Long versionId, Long campoId, String valor) {
        var version = versionRepo.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

        CampoValor cv = CampoValor.builder()
                .version(version)
                .campo(PlantillaCampo.builder().id(campoId).build())
                .valor(valor)
                .build();

        return valoresRepo.save(cv);
    }

    @Override
    public DocumentoAdjunto guardarAdjunto(Long versionId, MultipartFile file) {
        var version = versionRepo.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

        DocumentoAdjunto adj = DocumentoAdjunto.builder()
                .version(version)
                .tipo(file.getContentType())
                .descripcion(file.getOriginalFilename())
                .fechaSubida(java.time.LocalDateTime.now())
                .url("pendiente") // luego lo implementamos
                .build();

        return adjuntoRepo.save(adj);
    }

    @Override
    public HistoriaClinica obtenerHC(Long id) {
        return hcRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("HC no encontrada"));
    }
}
