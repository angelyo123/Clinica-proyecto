package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.dto.CampoValorConfirmado;
import com.historias_clinicas.hc.entidades.CampoValor;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import com.historias_clinicas.hc.entidades.PlantillaAccion;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.generadores.PdfGenerator;
import com.historias_clinicas.hc.repositorios.CampoValorRepository;
import com.historias_clinicas.hc.repositorios.HistoriaClinicaVersionRepository;
import com.historias_clinicas.hc.repositorios.PlantillaAccionRepo;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.servicios.word.PlantillaFillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final PlantillaAccionRepo plantillaAccionRepo;
    private final PlantillaFillService plantillaFillService;
    private final HistoriaClinicaVersionRepository versionRepo;

    // ============================================================
    // CONFIRMAR VALORES (jsonConfirmar → DTOs físicos)
    // ============================================================
    public List<CampoValorConfirmado> guardarValoresAcciones(
            HistoriaClinicaVersion version,
            Map<String, String> valoresPlano
    ) {

        List<CampoValorConfirmado> confirmados = new ArrayList<>();

        valoresPlano.forEach((accionIdRaw, textoFinal) -> {

            Long accionId;
            try {
                accionId = Long.valueOf(accionIdRaw);
            } catch (Exception e) {
                return;
            }

            PlantillaAccion accion = plantillaAccionRepo.findById(accionId)
                    .orElse(null);

            if (accion == null) return;

            CampoValorConfirmado dto = new CampoValorConfirmado();
            dto.setValor(textoFinal);
            dto.setIndexTabla(accion.getIndexTabla());
            dto.setIndexFila(accion.getIndexFila());
            dto.setIndexCelda(accion.getIndexColumna());
            dto.setIndexParrafo(null); // por ahora

            confirmados.add(dto);
        });

        return confirmados;
    }

    // ============================================================
    // GENERAR WORD
    // ============================================================
    public byte[] generarWord(
            HistoriaClinicaVersion version,
            List<CampoValorConfirmado> valores
    ) {

        try {
            byte[] plantillaBytes = version.getHistoriaClinica()
                    .getPlantilla()
                    .getArchivoOriginal();

            byte[] wordFinal = plantillaFillService.llenarWord(
                    plantillaBytes,
                    valores
            );

            version.setWordFinal(wordFinal);
            versionRepo.save(version);

            return wordFinal;

        } catch (Exception e) {
            throw new RuntimeException("Error generando Word", e);
        }
    }
}