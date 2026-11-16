package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.cognitive.TargetRegion;
import com.historias_clinicas.hc.dto.CampoValorConfirmado;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.extraction.dto.FieldValueAI;
import com.historias_clinicas.hc.extraction.mapper.FieldValueMapper;
import com.historias_clinicas.hc.extraction.service.NaturalLanguageFillService;
import com.historias_clinicas.hc.extraction.validation.FieldValueValidator;
import com.historias_clinicas.hc.repositorios.HistoriaClinicaVersionRepository;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.servicios.word.PlantillaFillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final PlantillaFillService plantillaFillService;
    private final HistoriaClinicaVersionRepository versionRepo;
    private final NaturalLanguageFillService naturalLanguageFillService;
    private final PlantillaCampoRepository campoRepo;

    // ============================================================
    // GENERAR WORD
    // ============================================================
    public byte[] generarWord(
            HistoriaClinicaVersion version,
            List<CampoValorConfirmado> valores,
            List<FieldDefinition> defs
    ) {

        try {
            byte[] plantillaBytes = version.getHistoriaClinica()
                    .getPlantilla()
                    .getArchivoOriginal();

            byte[] wordFinal = plantillaFillService.llenarWord(
                    plantillaBytes,
                    valores,
                    defs
            );

            version.setWordFinal(wordFinal);
            versionRepo.save(version);

            return wordFinal;

        } catch (Exception e) {
            throw new RuntimeException("Error generando Word", e);
        }
    }

    public List<CampoValorConfirmado> convertirStableIdsAMarcas(
            List<String> stableIds
    ) {
        List<CampoValorConfirmado> marcas = new ArrayList<>();

        for (String id : stableIds) {

            CampoValorConfirmado dto = new CampoValorConfirmado();
            dto.setStableId(id);
            dto.setValor("true");
            dto.setTipoDato("booleano");

            marcas.add(dto);
        }

        return marcas;
    }

    public byte[] generarWordConChecklist(
            HistoriaClinicaVersion version,
            List<CampoValorConfirmado> valoresTexto,
            List<String> stableIdsChecklist
    ) {

        List<CampoValorConfirmado> marcas =
                convertirStableIdsAMarcas(stableIdsChecklist);

        List<CampoValorConfirmado> todo = new ArrayList<>();
        todo.addAll(valoresTexto);
        todo.addAll(marcas);

        Long plantillaId = version.getHistoriaClinica()
                .getPlantilla()
                .getId();

        List<FieldDefinition> campos = reconstruirCampos(plantillaId);

        return generarWord(version, todo, campos);
    }

    public byte[] generarWordDesdeTexto(
            HistoriaClinicaVersion version,
            String texto
    ) {

        try {

            Long plantillaId = version.getHistoriaClinica()
                    .getPlantilla()
                    .getId();

            // 1️⃣ cargar campos del snapshot
            List<PlantillaCampo> camposDB =
                    campoRepo.findByPlantillaId(plantillaId);

            List<FieldDefinition> campos = reconstruirCampos(plantillaId);

            // 2️⃣ IA extrae valores
            List<FieldValueAI> valoresIA =
                    naturalLanguageFillService.process(texto, campos);

            valoresIA = FieldValueValidator.validate(valoresIA, campos);

            System.out.println("===== VALORES IA =====");

            valoresIA.forEach(v ->
                    System.out.println(v.getStableId() + " -> " + v.getValor())
            );

            // 🔥 eliminar duplicados
            valoresIA = normalizarValores(valoresIA);

            // 3️⃣ convertir a DTO interno
            List<CampoValorConfirmado> confirmados =
                    FieldValueMapper.map(valoresIA, campos);

            System.out.println("===== CONFIRMADOS (WRITE IDS) =====");

            confirmados.forEach(c ->
                    System.out.println("[WRITE] " + c.getStableId() + " -> " + c.getValor())
            );

            System.out.println("===== CONFIRMADOS =====");

            confirmados.forEach(c ->
                    System.out.println(c.getStableId() + " -> " + c.getValor())
            );

            // 4️⃣ generar Word
            return generarWord(version, confirmados, campos);

        } catch (Exception e) {

            e.printStackTrace();   // 🔥

            throw new RuntimeException(
                    "Error generando Word desde texto: " + e.getMessage(),
                    e
            );
        }
    }


    private List<FieldDefinition> reconstruirCampos(Long plantillaId) {

        List<PlantillaCampo> camposDB =
                campoRepo.findByPlantillaId(plantillaId);

        return camposDB.stream()
                .map(c -> {

                    FieldDefinition f = new FieldDefinition();

                    f.setStableId(c.getReadStableId());
                    f.setWriteStableId(c.getStableId());
                    f.setDescription(c.getDescripcion());
                    f.setDataType(c.getTipoDato());

                    f.setInteractionType(c.getInteractionType());
                    f.setPreservationMode(c.getPreservationMode());
                    f.setTargetSemantics(c.getTargetSemantics());

                    f.setWriteMode(c.getWriteMode());
                    f.setWriteValue(c.getWriteValue());
                    f.setExecutionHint(c.getExecutionHint());

                    f.setRewriteText(c.getRewriteText());

                    TargetRegion r = new TargetRegion();
                    r.setMode(c.getTargetRegionMode());
                    r.setAnchorText(c.getAnchorText());
                    r.setSecondAnchorText(c.getSecondAnchorText());
                    r.setRelativeRow(c.getRelativeRow());
                    r.setRelativeCol(c.getRelativeCol());
                    r.setReference(c.getReference());
                    r.setHint(c.getTargetHint());

                    f.setTargetRegion(r);

                    return f;
                })
                .toList();
    }

    private List<FieldValueAI> normalizarValores(List<FieldValueAI> valores) {

        Map<String, FieldValueAI> map = new LinkedHashMap<>();

        for (FieldValueAI v : valores) {

            if (v.getStableId() == null) continue;
            if (v.getValor() == null || v.getValor().length() < 2) continue;

            if (map.containsKey(v.getStableId())) {
                System.out.println("⚠️ duplicado IA: " + v.getStableId());
            }

            map.put(v.getStableId(), v);
        }



        return new ArrayList<>(map.values());
    }
}