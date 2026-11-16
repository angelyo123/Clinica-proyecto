package com.historias_clinicas.hc.controladores;

import com.historias_clinicas.hc.generadores.PdfGenerator;
import com.historias_clinicas.hc.ia.ImageInterpreter;
import com.historias_clinicas.hc.repositorios.HistoriaClinicaVersionRepository;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.servicios.DocumentoService;
import com.historias_clinicas.hc.servicios.PlantillaProcessorService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/hc/version")
@RequiredArgsConstructor
public class HCIAController {

    private final HistoriaClinicaVersionRepository versionRepo;
    private final DocumentoService documentoService;
    private final PlantillaCampoRepository campoRepo;
    private final PlantillaProcessorService plantillaProcessorService;
    private final ImageInterpreter imageInterpreter;
    private final PdfGenerator pdfGenerator;

    // -------------------------------------------------------------
    // PREVISUALIZACIÓN DESDE TEXTO (IA)
    // -------------------------------------------------------------
    @PostMapping("/{versionId}/ia/texto-preliminar")
    public ResponseEntity<?> interpretarTextoPreliminar(
            @PathVariable Long versionId,
            @RequestBody String texto
    ) {

        try {

            var version = versionRepo.findById(versionId)
                    .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

            Long plantillaId = version.getHistoriaClinica()
                    .getPlantilla()
                    .getId();

            // campos cognitivos congelados
            var campos = campoRepo.findByPlantillaId(plantillaId);

            List<Map<String, Object>> camposPlantilla = campos.stream()
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("stableId", c.getStableId());
                        m.put("descripcion", c.getDescripcion());
                        m.put("tipoDato", c.getTipoDato());
                        return m;
                    })
                    .toList();

            Map<String, Object> resultado = Map.of(
                    "campos", camposPlantilla,
                    "texto", texto
            );

            List<String> stableIdsChecklist =
                    plantillaProcessorService.procesarChecklists(
                            plantillaId,
                            texto
                    );

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Previsualización generada",
                    "data", Map.of(
                            "campos", camposPlantilla,
                            "stableIdsChecklist", stableIdsChecklist
                    )
            ));

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------
    // PREVISUALIZACIÓN DESDE IMAGEN (OCR + IA)
    // -------------------------------------------------------------
    @PostMapping("/{versionId}/ia/imagen-preliminar")
    public ResponseEntity<?> interpretarImagenPreliminar(
            @PathVariable Long versionId,
            @RequestParam MultipartFile file
    ) {

        try {

            var version = versionRepo.findById(versionId)
                    .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

            Long plantillaId = version.getHistoriaClinica()
                    .getPlantilla()
                    .getId();

            // OCR
            String texto = imageInterpreter.leerImagen(file);

            List<String> stableIdsChecklist =
                    plantillaProcessorService.procesarChecklists(
                            plantillaId,
                            texto
                    );

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Imagen interpretada",
                    "textoDetectado", texto,
                    "stableIdsChecklist", stableIdsChecklist
            ));

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------
    // GENERAR WORD DIRECTAMENTE DESDE TEXTO (PIPELINE COMPLETO)
    // -------------------------------------------------------------
    @PostMapping(
            value = "/{versionId}/llenar-texto",
            produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
    public ResponseEntity<byte[]> llenarDesdeTexto(
            @PathVariable Long versionId,
            @RequestBody String texto
    ) {

        try {

            var version = versionRepo.findById(versionId)
                    .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

            byte[] word =
                    documentoService.generarWordDesdeTexto(
                            version,
                            texto
                    );

            return ResponseEntity.ok()
                    .header(
                            "Content-Type",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
                    .header(
                            "Content-Disposition",
                            "attachment; filename=HC_" + versionId + ".docx"
                    )
                    .body(word);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(("ERROR: " + e.getMessage()).getBytes());
        }
    }

    // -------------------------------------------------------------
    // CHECKLIST DESDE TEXTO
    // -------------------------------------------------------------
    @PostMapping("/plantilla/{id}/checklist")
    public ResponseEntity<?> marcarChecklist(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {

        try {

            String textoClinico = body.get("texto");

            List<String> stableIds =
                    plantillaProcessorService.procesarChecklists(
                            id,
                            textoClinico
                    );

            return ResponseEntity.ok(Map.of(
                    "stableIds", stableIds,
                    "cantidad", stableIds.size()
            ));

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}