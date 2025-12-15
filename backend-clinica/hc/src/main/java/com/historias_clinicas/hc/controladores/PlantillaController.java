package com.historias_clinicas.hc.controladores;

import com.historias_clinicas.hc.entidades.Plantilla;
import com.historias_clinicas.hc.servicios.PlantillaProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hc/plantilla")
@RequiredArgsConstructor
public class PlantillaController {

    private final PlantillaProcessorService plantillaProcessorService;
    // ---------------------------------------------------------------
    // SUBIR PLANTILLA
    // ---------------------------------------------------------------
    @PostMapping("/subir")
    public ResponseEntity<?> subirPlantilla(@RequestParam MultipartFile file) throws Exception {

        Plantilla plantilla = plantillaProcessorService.procesarPlantilla(
                file.getBytes(),   // ← CORRECTO
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

        long inicio = System.currentTimeMillis();

        try {

            Map<String, Object> resultado =
                    plantillaProcessorService.analizarPlantilla(plantillaId);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Plantilla analizada correctamente",
                    "duracion_ms", System.currentTimeMillis() - inicio,
                    "data", resultado
            ));

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "detalle", e.toString()
            ));
        }
    }

}