package com.historias_clinicas.hc.controladores;

import com.historias_clinicas.hc.dto.CrearHCDTO;
import com.historias_clinicas.hc.servicios.HistoriaClinicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/hc")
@RequiredArgsConstructor
public class HCController {

    private final HistoriaClinicaService hcService;

    @PostMapping("/crear")
    public ResponseEntity<?> crearHC(@RequestBody CrearHCDTO dto) {
        var hc = hcService.crearHC(dto.getPacienteId(), dto.getMedicoId(), dto.getPlantillaId());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Historia clínica creada",
                "hcId", hc.getId()
        ));
    }

    @PostMapping("/{id}/version")
    public ResponseEntity<?> crearVersion(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Versión automática") String descripcion
    ) {

        var version = hcService.crearVersion(id, descripcion);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Versión creada correctamente",
                "versionId", version.getId()
        ));
    }

    @PostMapping("/version/{versionId}/campo/{campoId}")
    public ResponseEntity<?> guardarValor(
            @PathVariable Long versionId,
            @PathVariable Long campoId,
            @RequestBody String valor
    ) {
        return ResponseEntity.ok(
                hcService.guardarCampo(versionId, campoId, valor)
        );
    }

    @PostMapping("/version/{versionId}/adjunto")
    public ResponseEntity<?> subirAdjunto(
            @PathVariable Long versionId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                hcService.guardarAdjunto(versionId, file)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(
                hcService.obtenerHC(id)
        );
    }
}