package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.cognitive.TargetRegion;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.entidades.PlantillaSnapshot;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.repositorios.PlantillaSnapshotRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CognitivePersistenceService {

    private final PlantillaSnapshotRepo snapshotRepo;
    private final PlantillaCampoRepository campoRepo;

    @Transactional
    public void guardarSnapshot(
            Long plantillaId,
            String docHash,
            String modelVersion,
            List<FieldDefinition> fields
    ) {
        // 1) borrar anterior congelado (o mantener historial si quieres)
        campoRepo.deleteByPlantillaId(plantillaId);

        PlantillaSnapshot snapshot = PlantillaSnapshot.builder()
                .plantillaId(plantillaId)
                .docHash(docHash)
                .modelVersion(modelVersion)
                .createdAt(LocalDateTime.now())
                .build();

        snapshotRepo.save(snapshot);

        // 2) guardar fields
        for (FieldDefinition f : fields) {


            TargetRegion r = f.getTargetRegion();

            PlantillaCampo campo = PlantillaCampo.builder()
                    .plantillaId(plantillaId)
                    .stableId(f.getWriteStableId())
                    .readStableId(f.getStableId())
                    .descripcion(f.getDescription())
                    .tipoDato(f.getDataType())
                    .snapshotId(snapshot.getId())

                    .geom(extractGeom(f.getWriteStableId()))
                    .blockHash(extractBlockHash(f.getWriteStableId()))
                    .anchorHash(extractAnchor(f.getWriteStableId()))

                    .interactionType(f.getInteractionType())
                    .preservationMode(f.getPreservationMode())
                    .targetSemantics(f.getTargetSemantics())
                    .writeMode(f.getWriteMode())
                    .writeValue(f.getWriteValue())
                    .executionHint(f.getExecutionHint())

                    .targetRegionMode(r != null ? r.getMode() : null)
                    .anchorText(r != null ? r.getAnchorText() : null)
                    .secondAnchorText(r != null ? r.getSecondAnchorText() : null)
                    .relativeRow(r != null ? r.getRelativeRow() : null)
                    .relativeCol(r != null ? r.getRelativeCol() : null)
                    .reference(r != null ? r.getReference() : null)
                    .targetHint(r != null ? r.getHint() : null)

                    .rewriteText(f.getRewriteText())

                    .build();

            campoRepo.save(campo);
        }
    }

    private String extractGeom(String stableId) {

        int start = stableId.indexOf("/R");
        int end = stableId.indexOf("#");

        if (start == -1 || end == -1) return "";

        return stableId.substring(start + 1, end);
    }

    private String extractBlockHash(String stableId) {

        int slash = stableId.indexOf("/");

        if (slash == -1) return "";

        return stableId.substring(1, slash);
    }

    private String extractAnchor(String stableId) {

        int hash = stableId.indexOf("#");

        if (hash == -1) return "";

        return stableId.substring(hash + 1);
    }
}