package com.historias_clinicas.hc.controladores;

import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.generadores.PdfGenerator;
import com.historias_clinicas.hc.ia.IaService;
import com.historias_clinicas.hc.ia.ImageInterpreter;
import com.historias_clinicas.hc.ia.MappingEngine;
import com.historias_clinicas.hc.ia.TextInterpreter;
import com.historias_clinicas.hc.repositorios.CampoValorRepository;
import com.historias_clinicas.hc.repositorios.PlantillaCampoRepository;
import com.historias_clinicas.hc.servicios.DocumentoService;

import com.historias_clinicas.hc.repositorios.HistoriaClinicaVersionRepository;

import com.historias_clinicas.hc.servicios.PlantillaProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hc/version")
@RequiredArgsConstructor
public class HCIAController {

    private final TextInterpreter textInterpreter;
    private final ImageInterpreter imageInterpreter;
    private final MappingEngine mappingEngine;
    private final HistoriaClinicaVersionRepository versionRepo;
    private final DocumentoService documentoService;
    private final PlantillaCampoRepository campoRepo;
    private final IaService iaService;
    private final PdfGenerator pdfGenerator;
    private final PlantillaProcessorService plantillaProcessorService;

    // -------------------------------------------------------------
    // 1. PREVISUALIZACIÓN DESDE TEXTO
    // -------------------------------------------------------------
    @PostMapping("/{versionId}/ia/texto-preliminar")
    public ResponseEntity<?> interpretarTextoPreliminar(
            @PathVariable Long versionId,
            @RequestBody String texto
    ) {
        try {

            var version = versionRepo.findById(versionId)
                    .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

            Long plantillaId = version.getHistoriaClinica().getPlantilla().getId();

            // 1. OBTENER TODOS LOS CAMPOS DETECTADOS POR /analizar
            List<PlantillaCampo> campos = campoRepo.findBySeccion_Plantilla_Id(plantillaId);

            // 2. CONSTRUIR LA LISTA QUE LA IA NECESITA
            List<Map<String,Object>> camposPlantilla = campos.stream()
                    .map(c -> {
                        Map<String,Object> m = new HashMap<>();
                        m.put("nombre", c.getNombreCampo());
                        m.put("textoOriginal", c.getTextoOriginal());
                        m.put("tabla", c.getIndexTabla());
                        m.put("fila", c.getIndexFila());
                        m.put("columna", c.getIndexCelda());
                        m.put("parrafo", c.getIndexParrafo());
                        return m;
                    })
                    .toList();


            // 3. IA interpreta texto usando ESTA LISTA (campo → textoOriginal completado)
            Map<String,Object> jsonIA = textInterpreter.interpretarTexto(
                    texto,
                    camposPlantilla
            );

            // 4. LISTA bonita para el front (con IDs)
            List<Map<String,Object>> lista = campos.stream()
                    .map(c -> Map.of(
                            "campoId", c.getId(),
                            "nombre", c.getNombreCampo(),
                            "valor", jsonIA.getOrDefault(c.getNombreCampo(), c.getTextoOriginal())
                    ))
                    .toList();

            // 5. JSON EXACTO que /confirmar necesita
            Map<String,String> jsonConfirmar = new LinkedHashMap<>();
            jsonIA.forEach((k,v) -> jsonConfirmar.put(k, v.toString()));

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Previsualización generada desde texto",
                    "data", Map.of(
                            "lista", lista,
                            "jsonConfirmar", jsonConfirmar
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------
    // 2. PREVISUALIZACIÓN DESDE IMAGEN
    // -------------------------------------------------------------
    @PostMapping("/{versionId}/ia/imagen-preliminar")
    public ResponseEntity<?> interpretarImagenPreliminar(
            @PathVariable Long versionId,
            @RequestParam MultipartFile file
    ) {
        try {

            var version = versionRepo.findById(versionId)
                    .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

            Long plantillaId = version.getHistoriaClinica().getPlantilla().getId();

            // 1. Leer texto desde la imagen (OCR IA)
            String texto = imageInterpreter.leerImagen(file);

            // 2. Obtener lista de campos EXACTOS detectados por POI
            List<Map<String,Object>> camposPlantilla = campoRepo.findBySeccion_Plantilla_Id(plantillaId)
                    .stream()
                    .map(c -> {
                        Map<String,Object> m = new HashMap<>();
                        m.put("nombre", c.getNombreCampo());
                        m.put("textoOriginal", c.getTextoOriginal());
                        m.put("tabla", c.getIndexTabla());
                        m.put("fila", c.getIndexFila());
                        m.put("columna", c.getIndexCelda());
                        m.put("parrafo", c.getIndexParrafo());
                        return m;
                    })
                    .collect(Collectors.toList());


            // 3. IA interpreta texto usando ESA LISTA (campo → valor)
            Map<String,Object> jsonIA = textInterpreter.interpretarTexto(
                    texto,
                    camposPlantilla
            );

            // 4. Convertir a Map plano (String → String)
            Map<String,String> datosPlano = mappingEngine.aPlano(jsonIA);

            // 5. Mapear IA → Plantilla
            var mapeo = mappingEngine.mapearDatosAPlantilla(datosPlano, plantillaId);

            // >>>>>> MISMA RESPUESTA QUE TEXTO-PRELIMINAR <<<<<<

            // 5A. LISTA para el front
            List<Map<String, Object>> lista = mapeo.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("campoId", e.getKey().getId());
                        item.put("nombre", e.getKey().getNombreCampo());
                        item.put("valor", e.getValue());
                        return item;
                    })
                    .toList();

            // 5B. JSON EXACTO que /confirmar necesita
            Map<String, String> jsonConfirmar = new LinkedHashMap<>();
            mapeo.forEach((campo, valor) -> {
                jsonConfirmar.put(campo.getNombreCampo(), valor);
            });

            // 6. MISMA estructura
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Previsualización generada desde imagen",
                    "data", Map.of(
                            "lista", lista,
                            "jsonConfirmar", jsonConfirmar
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------
    // 3. CONFIRMAR → GUARDAR → WORD → PDF
    // -------------------------------------------------------------
    @PostMapping(
            value = "/{versionId}/confirmar",
            produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
    public ResponseEntity<byte[]> confirmarDescarga(
            @PathVariable Long versionId,
            @RequestBody Map<String, String> valoresPlano
    ) {
        try {

            var version = versionRepo.findById(versionId)
                    .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

            Map<PlantillaCampo, String> valoresIA =
                    documentoService.guardarValores(version, valoresPlano);

            byte[] word = documentoService.generarWord(version, valoresIA);

            return ResponseEntity.ok()
                    .header("Content-Type",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .header("Content-Disposition",
                            "attachment; filename=HC_" + versionId + ".docx")
                    .body(word);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("ERROR: " + e.getMessage()).getBytes());
        }
    }
}