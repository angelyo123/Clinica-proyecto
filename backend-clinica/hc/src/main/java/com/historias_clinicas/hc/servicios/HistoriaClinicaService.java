package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.entidades.CampoValor;
import com.historias_clinicas.hc.entidades.DocumentoAdjunto;
import com.historias_clinicas.hc.entidades.HistoriaClinica;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import org.springframework.web.multipart.MultipartFile;

public interface HistoriaClinicaService {

    HistoriaClinica crearHC(Long pacienteId, Long medicoId, Long plantillaId);

    HistoriaClinicaVersion crearVersion(Long hcId, String descripcion);

    CampoValor guardarCampo(Long versionId, Long campoId, String valor);

    DocumentoAdjunto guardarAdjunto(Long versionId, MultipartFile file);

    HistoriaClinica obtenerHC(Long id);
}
