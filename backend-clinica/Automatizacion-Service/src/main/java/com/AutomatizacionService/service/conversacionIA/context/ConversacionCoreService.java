package com.AutomatizacionService.service.conversacionIA.context;

import com.AutomatizacionService.service.RazonamientoIAService;
import com.AutomatizacionService.service.conversacionIA.AccionIARegistry;
import com.AutomatizacionService.service.conversacionIA.ActualizacionMedicosService;
import com.AutomatizacionService.service.conversacionIA.ConversacionPromptBuilder;
import com.AutomatizacionService.service.orquestador.DeepSeekService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConversacionCoreService {

    @Autowired private DeepSeekService deepSeekService;
    @Autowired private ConversacionContextService contextService;
    @Autowired private ConversacionPromptBuilder promptBuilder;
    @Autowired private AccionIARegistry accionRegistry;
    @Autowired private ActualizacionMedicosService actualizacionMedicosService;
    @Autowired private RazonamientoIAService razonamientoIAService;

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> ejecutarFlujoConversacion(Map<String, Object> solicitud) {
        try {
            Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());
            String mensaje = solicitud.get("mensaje").toString();

            // 🧠 Contexto previo
            String contextoPrevio = contextService.construirContextoConversacion(pacienteId);
            String ultimoJson = contextService.obtenerUltimoData(pacienteId);
            String memoria = contextService.construirMemoriaPaciente(pacienteId);

            // 🧩 Paso 1: la IA decide las acciones necesarias
            String prompt = promptBuilder.construirPrompt(mensaje, contextoPrevio, ultimoJson, memoria);
            String respuestaIA = deepSeekService.generarTexto(prompt);
            JsonNode root = mapper.readTree(respuestaIA);
            String contenido = root.path("choices").get(0).path("message").path("content").asText();
            System.out.println("🧠 [RAW RESPUESTA IA] " + contenido);

            // 🧹 Limpieza de formato Markdown de la respuesta IA
            contenido = contenido
                    .replaceAll("^```json", "")
                    .replaceAll("^```", "")
                    .replaceAll("```$", "")
                    .replaceAll("```", "")
                    .trim();
            System.out.println("🧠 [LIMPIO RESPUESTA IA] " + contenido);

            Map<String, Object> decision = mapper.readValue(contenido, Map.class);

            // 💾 Guardar decisión inicial
            contextService.guardar(
                    pacienteId,
                    mensaje,
                    decision.getOrDefault("respuesta", "").toString(),
                    decision.get("data")
            );

            // ⚙️ Obtener lista de acciones
            List<Map<String, Object>> acciones = (List<Map<String, Object>>) decision.get("acciones");
            if (acciones == null || acciones.isEmpty()) {
                String accionSimple = decision.getOrDefault("accion", "").toString();
                if (accionSimple.isBlank()) {
                    return Map.of("mensaje", decision.getOrDefault("respuesta", "No entendí tu mensaje."));
                }
                acciones = List.of(Map.of(
                        "accion", accionSimple,
                        "parametros", decision.getOrDefault("parametros", Map.of())
                ));
            }

            // 🚀 Ejecutar cada acción pedida por la IA
            Map<String, Object> dataFusionada = new LinkedHashMap<>();

            for (Map<String, Object> accionInfo : acciones) {
                String accion = (String) accionInfo.get("accion");
                Map<String, Object> params = (Map<String, Object>) accionInfo.getOrDefault("parametros", Map.of());

                var accionIA = accionRegistry.getAcciones().get(accion);
                if (accionIA == null) {
                    System.out.println("⚠️ Acción no registrada: " + accion);
                    continue;
                }

                System.out.println("🧠 Ejecutando acción dinámica: " + accionIA.nombre());
                Map<String, Object> resultado = accionIA.metodo().apply(pacienteId, params);

                // 🔧 Fusión semántica por nombre de acción
                switch (accion) {
                    case "listar_medicos" -> dataFusionada.put("medicos", resultado.get("data"));
                    case "listar_horarios" -> dataFusionada.put("horarios", resultado.get("data"));
                    default -> dataFusionada.put(accion, resultado.get("data"));
                }
            }

            String razonamientoFinal = decision.getOrDefault("respuesta", "").toString();
            if (decision.containsKey("acciones")) {
                razonamientoFinal = "";
            }

            // 🤔 Si hay datos estructurados, IA razona con ellos
            if (!dataFusionada.isEmpty()) {
                String datosJson = mapper.writeValueAsString(dataFusionada);

                String promptFinal = promptBuilder.construirPrompt(
                        "Analiza los resultados obtenidos de las acciones ejecutadas y responde al paciente según su mensaje original.",
                        contextoPrevio,
                        datosJson,
                        memoria
                );

                String respuestaFinal = deepSeekService.generarTexto(promptFinal);
                JsonNode rootFinal = mapper.readTree(respuestaFinal);
                String contenidoFinal = rootFinal.path("choices").get(0).path("message").path("content").asText();

                // 💾 Guardar conversación final
                contextService.guardar(pacienteId, mensaje, contenidoFinal, dataFusionada);

                return Map.of("mensaje", contenidoFinal, "data", dataFusionada);
            }

            // Si no hay datos, devolvemos la respuesta base
            return Map.of("mensaje", razonamientoFinal);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error en conversación", "detalle", e.getMessage());
        }
    }
}