package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.entidades.CampoValor;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.generadores.PdfGenerator;
import com.historias_clinicas.hc.generadores.WordGenerator;
import com.historias_clinicas.hc.ia.MappingEngine;
import com.historias_clinicas.hc.repositorios.CampoValorRepository;
import com.historias_clinicas.hc.repositorios.HistoriaClinicaVersionRepository;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final CampoValorRepository campoValorRepo;
    private final PlantillaCampoRepository campoRepo;
    private final WordGenerator wordGenerator;
    private final PdfGenerator pdfGenerator;
    private final HistoriaClinicaVersionRepository versionRepo;

    // ============================================================================
    // GUARDAR VALORES CONFIRMADOS (jsonConfirmar → BD)
    // ============================================================================
    public Map<PlantillaCampo, String> guardarValores(
            HistoriaClinicaVersion version,
            Map<String, String> valoresPlano
    ) {

        if (version.getHistoriaClinica().getPlantilla() == null)
            throw new RuntimeException("La historia clínica no tiene plantilla asociada.");

        Long plantillaId = version.getHistoriaClinica().getPlantilla().getId();

        // 1️⃣ Traer TODOS los campos de la plantilla
        List<PlantillaCampo> campos = campoRepo.findBySeccion_Plantilla_Id(plantillaId);

        // 2️⃣ Convertir jsonConfirmar → Map<PlantillaCampo,String>
        Map<PlantillaCampo, String> valoresIA = new LinkedHashMap<>();

        valoresPlano.forEach((nombreCampo, textoCompletado) -> {

            // Buscar el campo exacto por nombre
            PlantillaCampo campo = campos.stream()
                    .filter(c -> c.getNombreCampo().equals(nombreCampo))
                    .findFirst()
                    .orElse(null);

            if (campo == null) {
                // No existe el campo — simplemente ignorar
                return;
            }

            valoresIA.put(campo, textoCompletado);

            // Guardar / actualizar en BD (evitar duplicados)
            CampoValor existente = campoValorRepo
                    .findByVersionIdAndCampoId(version.getId(), campo.getId())
                    .orElse(null);

            if (existente == null) {
                existente = CampoValor.builder()
                        .version(version)
                        .campo(campo)
                        .build();
            }

            existente.setValor(textoCompletado);
            campoValorRepo.save(existente);
        });

        return valoresIA;
    }

    public byte[] generarWord(HistoriaClinicaVersion version,
                              Map<PlantillaCampo, String> valoresIA) {

        try {
            if (version.getHistoriaClinica().getPlantilla() == null)
                throw new RuntimeException("La versión no tiene plantilla asociada.");

            // 1️⃣ obtener bytes del Word plantilla original
            byte[] plantillaBytes = version.getHistoriaClinica()
                    .getPlantilla()
                    .getArchivoOriginal();

            // 2️⃣ generar Word COMPLETADO
            byte[] wordFinal = wordGenerator.generarDocumento(plantillaBytes, valoresIA);

            // 3️⃣ guardar en BD solo si quieres
            version.setWordFinal(wordFinal);
            versionRepo.save(version);

            return wordFinal;

        } catch (Exception e) {
            e.printStackTrace();   // <-- AGREGA ESTO
            throw new RuntimeException("Error generando Word: " + e.getMessage(), e);
        }
    }
}
