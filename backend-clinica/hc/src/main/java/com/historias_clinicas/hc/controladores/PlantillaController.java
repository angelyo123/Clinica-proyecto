package com.historias_clinicas.hc.controladores;

import com.historias_clinicas.hc.entidades.Plantilla;
import com.historias_clinicas.hc.servicios.PlantillaExtractorService;
import com.historias_clinicas.hc.servicios.PlantillaProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/hc/plantilla")
@RequiredArgsConstructor
public class PlantillaController {

    private final PlantillaProcessorService plantillaProcessorService;
    private final PlantillaExtractorService plantillaExtractorService;

    // ---------------------------------------------------------------
    // SUBIR PLANTILLA
    // ---------------------------------------------------------------
    @PostMapping("/subir")
    public ResponseEntity<?> subirPlantilla(@RequestParam MultipartFile file) throws Exception {

        Plantilla plantilla = plantillaProcessorService.procesarPlantilla(
                file,
                file.getOriginalFilename()
        );

        return ResponseEntity.ok(Map.of(
                "mensaje", "Plantilla subida correctamente",
                "idPlantilla", plantilla.getId()
        ));
    }

    // ---------------------------------------------------------------
    // ANALIZAR PLANTILLA (IA + POSICIONES WORD)
    // ---------------------------------------------------------------
    @PostMapping("/{plantillaId}/analizar")
    public ResponseEntity<?> analizarPlantilla(@PathVariable Long plantillaId) {

        try {
            Map<String, Object> resultado =
                    plantillaProcessorService.analizarPlantilla(plantillaId);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Plantilla analizada correctamente",
                    "data", resultado
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ---------------------------------------------------------------
    // EXTRAER PLANTILLA DESDE WORD LLENO
    // ---------------------------------------------------------------
    @PostMapping(
            value = "/extraer",
            produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
    public ResponseEntity<byte[]> extraer(@RequestParam MultipartFile file) throws Exception {

        byte[] plantilla = plantillaExtractorService.generarPlantillaDesdeWord(file.getBytes());

        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .header("Content-Disposition", "attachment; filename=plantilla_generada.docx")
                .body(plantilla);
    }

}