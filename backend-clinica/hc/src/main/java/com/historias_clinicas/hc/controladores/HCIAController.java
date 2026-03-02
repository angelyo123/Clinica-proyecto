package com.historias_clinicas.hc.controladores;

import com.historias_clinicas.hc.dto.CampoValorConfirmado;
import com.historias_clinicas.hc.entidades.ConfirmarRequest;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import com.historias_clinicas.hc.entidades.PlantillaAccion;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.generadores.PdfGenerator;
import com.historias_clinicas.hc.ia.IaService;
import com.historias_clinicas.hc.ia.ImageInterpreter;
import com.historias_clinicas.hc.ia.MappingEngine;
import com.historias_clinicas.hc.ia.TextInterpreter;
import com.historias_clinicas.hc.repositorios.CampoValorRepository;
import com.historias_clinicas.hc.repositorios.PlantillaAccionRepo;
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
    private final PlantillaAccionRepo plantillaAccionRepo;
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

            // 1️⃣ TRAER TODOS LOS CAMPOS DE LA PLANTILLA
            List<PlantillaAccion> acciones =
                    plantillaAccionRepo.findByPlantillaId(plantillaId);

            // 2️⃣ ARMAR TODOS LOS DATOS NECESARIOS PARA IA
            List<Map<String,Object>> camposPlantilla = acciones.stream()
                    .map(a -> {
                        Map<String,Object> m = new LinkedHashMap<>();
                        m.put("id", a.getId());                     // 🔑 clave
                        m.put("textoOriginal", a.getTextoOriginal());
                        m.put("descripcion", a.getDescripcion());
                        m.put("tabla", a.getIndexTabla());
                        m.put("fila", a.getIndexFila());
                        m.put("columna", a.getIndexColumna());
                        return m;
                    })
                    .toList();

            // 3️⃣ IA INTERPRETA EL TEXTO
            Map<String,Object> jsonIA = textInterpreter.interpretarTexto(texto, camposPlantilla);

            // 5️⃣ JSON PARA CONFIRMAR — SOLO LOS QUE TIENEN VALOR
            Map<String,String> jsonConfirmar = new LinkedHashMap<>();
            jsonIA.forEach((k,v) -> {
                if (v != null && !v.toString().isBlank()) {
                    jsonConfirmar.put(k, v.toString());
                }
            });

            List<Map<String,Object>> accionesChecklist =
                    plantillaProcessorService.procesarChecklists(plantillaId, texto);

            return ResponseEntity.ok(Map.of(
                    "mensaje","Previsualización generada desde texto",
                    "data", Map.of(
                            "jsonConfirmar", jsonConfirmar,
                            "accionesChecklist", accionesChecklist
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

    @PostMapping(
            value = "/{versionId}/confirmar",
            produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
    public ResponseEntity<byte[]> confirmarDescarga(
            @PathVariable Long versionId,
            @RequestBody ConfirmarRequest req
    ) {
        try {

            var version = versionRepo.findById(versionId)
                    .orElseThrow(() -> new RuntimeException("Versión no encontrada"));

            Long plantillaId = version.getHistoriaClinica().getPlantilla().getId();

            // 1) valores texto (ids BD -> coordenadas)
            List<CampoValorConfirmado> valoresTexto =
                    documentoService.guardarValoresAcciones(version, req.getValoresPlano());

            // 2) acciones checklist (runtime)
            List<Map<String, Object>> accionesChecklist =
                    plantillaProcessorService.procesarChecklists(plantillaId, req.getTextoClinico());

            // 3) generar word con TODO
            byte[] word = documentoService.generarWordConChecklist(
                    version,
                    valoresTexto,
                    accionesChecklist
            );

            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .header("Content-Disposition", "attachment; filename=HC_" + versionId + ".docx")
                    .body(word);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("ERROR: " + e.getMessage()).getBytes());
        }
    }

    // -------------------------------------------------------------
// DEBUG: DETECTAR TABLAS REALES
// -------------------------------------------------------------
    @GetMapping("/plantilla/{plantillaId}/detectar-tablas")
    public ResponseEntity<?> detectarTablas(
            @PathVariable Long plantillaId
    ) {
        try {

            Map<String, Object> estructura =
                    plantillaProcessorService.extraerEstructuraPoi(plantillaId)
                            .get("estructura") instanceof Map<?,?> e
                            ? (Map<String, Object>) e
                            : Map.of();

            List<Map<String, Object>> tablas =
                    (List<Map<String, Object>>) estructura.getOrDefault("tablas", List.of());

            List<Object> resultado = new ArrayList<>();

            for (Map<String, Object> tabla : tablas) {

                List<Map<String, Object>> detectadas =
                        plantillaProcessorService.detectarTablasRealesEnTabla(tabla);

                if (!detectadas.isEmpty()) {

                    for (Map<String, Object> sub : detectadas) {

                        String tipo = (String) sub.get("tipo");

                        if ("CHECKLIST".equals(tipo)) {

                            List<Map<String, Object>> items =
                                    (List<Map<String, Object>>) sub.getOrDefault("items", List.of());

                            resultado.add(Map.of(
                                    "tablaFisica", tabla.get("tabla"),
                                    "tipo", "CHECKLIST",
                                    "cantidadItems", items.size(),
                                    "subtabla", sub
                            ));

                        } else {

                            List<Map<String, Object>> filas =
                                    (List<Map<String, Object>>) sub.getOrDefault("filas", List.of());

                            if (filas.isEmpty()) continue;

                            resultado.add(Map.of(
                                    "tablaFisica", tabla.get("tabla"),
                                    "tipo", "TABLA",
                                    "filasDetectadas", filas.size(),
                                    "filaInicio", filas.get(0).get("fila"),
                                    "filaFin", filas.get(filas.size() - 1).get("fila"),
                                    "subtabla", sub
                            ));
                        }
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Tablas reales detectadas",
                    "cantidad", resultado.size(),
                    "tablas", resultado
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/plantilla/{id}/checklist")
    public ResponseEntity<?> marcarChecklist(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) throws Exception {

        String textoClinico = body.get("texto");

        List<Map<String, Object>> acciones =
                plantillaProcessorService.procesarChecklists(id, textoClinico);

        return ResponseEntity.ok(acciones);
    }
}