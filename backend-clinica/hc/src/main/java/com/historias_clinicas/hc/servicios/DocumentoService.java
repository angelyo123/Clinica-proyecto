package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.entidades.CampoValor;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.generadores.PdfGenerator;
import com.historias_clinicas.hc.generadores.WordGenerator;
import com.historias_clinicas.hc.ia.MappingEngine;
import com.historias_clinicas.hc.repositorios.CampoValorRepository;
import com.historias_clinicas.hc.repositorios.HistoriaClinicaVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final CampoValorRepository campoValorRepo;
    private final WordGenerator wordGenerator;
    private final PdfGenerator pdfGenerator;
    private final MappingEngine mappingEngine;
    private final HistoriaClinicaVersionRepository versionRepo;


    // ============================================================
    // GUARDAR VALORES CONFIRMADOS
    // ============================================================
    public Map<PlantillaCampo, String> guardarValores(
            HistoriaClinicaVersion version,
            Map<String, String> valoresPlano
    ) {

        if (version.getHistoriaClinica().getPlantilla() == null)
            throw new RuntimeException("La historia clínica no tiene plantilla asociada.");

        Long plantillaId = version.getHistoriaClinica().getPlantilla().getId();

        // 1. Convertir claves (String) → PlantillaCampo mediante Matching exacto
        Map<PlantillaCampo, String> valoresIA =
                mappingEngine.mapearDatosAPlantilla(valoresPlano, plantillaId);

        // 2. Guardar en BD (evitar duplicados)
        valoresIA.forEach((campo, valor) -> {

            CampoValor existente = campoValorRepo
                    .findByVersionIdAndCampoId(version.getId(), campo.getId())
                    .orElse(null);

            if (existente == null) {
                existente = CampoValor.builder()
                        .version(version)
                        .campo(campo)
                        .build();
            }

            existente.setValor(valor);
            campoValorRepo.save(existente);
        });

        return valoresIA;
    }


    // ============================================================
    // GENERAR PDF FINAL
    // ============================================================
    public byte[] generarPdf(HistoriaClinicaVersion version,
                             Map<PlantillaCampo, String> valoresIA) {

        try {
            if (version.getHistoriaClinica().getPlantilla() == null)
                throw new RuntimeException("La versión no tiene plantilla asociada.");

            // 1. obtener bytes de la plantilla original
            byte[] plantillaBytes = version.getHistoriaClinica()
                    .getPlantilla()
                    .getArchivoOriginal();

            // 2. generar word final con datos rellenos
            byte[] wordFinal = wordGenerator.generarDocumento(plantillaBytes, valoresIA);

            // 3. convertir word final a PDF
            byte[] pdfFinal = pdfGenerator.convertToPdf(wordFinal);

            // 4. Persistir en BD
            version.setWordFinal(wordFinal);
            version.setPdfFinal(pdfFinal);
            versionRepo.save(version);

            return pdfFinal;

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

}
