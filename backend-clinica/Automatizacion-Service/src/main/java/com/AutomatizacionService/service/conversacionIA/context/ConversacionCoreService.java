package com.AutomatizacionService.service.conversacionIA.context;

import com.AutomatizacionService.service.RazonamientoIAService;
import com.AutomatizacionService.service.conversacionIA.AccionIARegistry;
import com.AutomatizacionService.service.conversacionIA.ActualizacionMedicosService;
import com.AutomatizacionService.service.conversacionIA.ConversacionPromptBuilder;
import com.AutomatizacionService.service.conversacionMedico.ConversacionPromptBuilderMedico;
import com.AutomatizacionService.service.conversacionMedico.RegistroIAMedicoService;
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

    @Autowired
    private DeepSeekService deepSeekService;
    @Autowired
    private ConversacionContextService contextService;
    @Autowired
    private ConversacionPromptBuilder promptBuilder;
    @Autowired
    private AccionIARegistry accionRegistry;
    @Autowired
    private ActualizacionMedicosService actualizacionMedicosService;
    @Autowired
    private RazonamientoIAService razonamientoIAService;
    @Autowired
    private ConversacionPromptBuilderMedico promptBuilderMedico;
    @Autowired
    private RegistroIAMedicoService registroIAMedicoService;


    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> ejecutarFlujoConversacion(Map<String, Object> solicitud) {
        try {
            Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());
            String mensaje = solicitud.get("mensaje").toString();

            System.out.println("💬 [Conversación] Paciente ID: " + pacienteId + " → " + mensaje);

            // 🧠 Contexto y memoria previos (base del aprendizaje)
            String contextoPrevio = contextService.construirContextoConversacion(pacienteId);
            String memoria = contextService.construirMemoriaPaciente(pacienteId);

            // 🚀 Acción base para obtener entorno de datos actual
            var accion = accionRegistry.getAcciones().get("listar_medicos_y_horarios");
            Map<String, Object> resultado = accion.metodo().apply(pacienteId, Map.of());
            Map<String, Object> dataFusionada = (Map<String, Object>) resultado.get("data");

            // 🧩 Crear prompt actualizado con contexto + memoria
            String datosJson = mapper.writeValueAsString(dataFusionada);
            String prompt = promptBuilder.construirPrompt(mensaje, contextoPrevio, datosJson, memoria);

            // 🔮 Llamar al modelo IA (DeepSeek)
            String respuestaIA = deepSeekService.generarTexto(prompt);
            JsonNode root = mapper.readTree(respuestaIA);
            String contenido = root.path("choices").get(0).path("message").path("content").asText();

            System.out.println("🤖 [IA] Respuesta generada:\n" + contenido);

            // 💾 Guardar interacción inicial
            contextService.guardar(pacienteId, mensaje, contenido, dataFusionada);

            // 🧩 Intentar ejecutar acciones sugeridas por la IA
            try {
                int start = contenido.indexOf("{");
                int end = contenido.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    String posibleJson = contenido.substring(start, end + 1);
                    JsonNode jsonNode = mapper.readTree(posibleJson);

                    if (jsonNode.has("acciones")) {
                        for (JsonNode accionNode : jsonNode.get("acciones")) {
                            String nombreAccion = accionNode.path("accion").asText();
                            JsonNode paramsNode = accionNode.path("parametros");

                            // 🧠 Aprendizaje: validar si la acción existe
                            if (accionRegistry.getAcciones().containsKey(nombreAccion)) {
                                System.out.println("⚙️ Ejecutando acción automática sugerida por la IA: " + nombreAccion);
                                Map<String, Object> params = mapper.convertValue(paramsNode, Map.class);

                                Map<String, Object> resultadoAccion = accionRegistry.getAcciones()
                                        .get(nombreAccion)
                                        .metodo()
                                        .apply(pacienteId, params);

                                System.out.println("📦 Resultado acción " + nombreAccion + ": " + resultadoAccion);

                                if (resultadoAccion != null && resultadoAccion.containsKey("mensaje")) {
                                    contenido += "\n\n" + resultadoAccion.get("mensaje");
                                }

                                // 💾 Guardar ejecución de acción correcta
                                contextService.guardar(
                                        pacienteId,
                                        "[IA ejecutó acción: " + nombreAccion + "]",
                                        String.valueOf(resultadoAccion),
                                        dataFusionada
                                );

                            } else {
                                // ⚠️ Acción desconocida → generar feedback de aprendizaje
                                System.out.println("⚠️ Acción desconocida sugerida por IA: " + nombreAccion);

                                String feedback = """
                                La IA sugirió una acción no registrada: "%s".
                                Se registrará este intento para que aprenda en futuras conversaciones.
                                """.formatted(nombreAccion);

                                contextService.guardar(
                                        pacienteId,
                                        "[IA propuso acción desconocida]",
                                        feedback,
                                        dataFusionada
                                );

                                // 🔁 Añadimos nota reflexiva para su contexto futuro
                                contenido += "\n\n🤔 Parece que intenté usar una acción no disponible (" + nombreAccion +
                                        "). Aprenderé a usar las disponibles correctamente la próxima vez.";
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("❌ Error al intentar ejecutar acción sugerida por IA: " + ex.getMessage());
                ex.printStackTrace();
            }

            // ✅ Respuesta final consolidada
            return Map.of(
                    "mensaje", contenido,
                    "data", dataFusionada
            );

        } catch (Exception e) {
            System.err.println("💥 Error en flujo de conversación: " + e.getMessage());
            e.printStackTrace();
            return Map.of(
                    "error", "Error en conversación",
                    "detalle", e.getMessage()
            );
        }
    }



    public Map<String, Object> ejecutarFlujoConversacionMedico(Map<String, Object> solicitud, Map<String, Object> dataFusionada) {
        try {
            Long medicoId = Long.valueOf(solicitud.get("medicoId").toString());
            String mensaje = solicitud.get("mensaje").toString();

            System.out.println("💬 [Conversación-MÉDICO] ID: " + medicoId + " → " + mensaje);

            // 🧠 Construir contexto y memoria previos
            String contextoPrevio = contextService.construirContextoConversacion(medicoId);
            String memoria = contextService.construirMemoriaMedico(medicoId);

            // 📦 Serializar datos combinados (citas + horarios)
            String datosJson = mapper.writeValueAsString(dataFusionada);

            // 🧩 Construir prompt principal
            String prompt = promptBuilderMedico.construirPrompt(mensaje, contextoPrevio, datosJson, memoria);

            // 🔮 Llamar al motor IA
            String respuestaIA = deepSeekService.generarTexto(prompt);
            JsonNode root = mapper.readTree(respuestaIA);
            String contenido = root.path("choices").get(0).path("message").path("content").asText();

            System.out.println("🤖 [IA-MÉDICO] Respuesta generada:\n" + contenido);

            // 💾 Guardar conversación inicial
            contextService.guardar(medicoId, mensaje, contenido, dataFusionada);
            registroIAMedicoService.guardar(medicoId, mensaje, contenido);

            // ⚙️ Detección y ejecución de acciones automáticas
            boolean seEjecutoAccion = false;

            try {
                int start = contenido.indexOf("{");
                int end = contenido.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    String posibleJson = contenido.substring(start, end + 1);
                    JsonNode jsonNode = mapper.readTree(posibleJson);

                    if (jsonNode.has("acciones")) {
                        for (JsonNode accionNode : jsonNode.get("acciones")) {
                            String nombreAccion = accionNode.path("accion").asText();
                            JsonNode paramsNode = accionNode.path("parametros");

                            if (accionRegistry.getAcciones().containsKey(nombreAccion)) {
                                System.out.println("⚙️ Ejecutando acción automática (médico): " + nombreAccion);
                                Map<String, Object> params = mapper.convertValue(paramsNode, Map.class);

                                // Ejecutar acción registrada
                                Map<String, Object> resultadoAccion = accionRegistry.getAcciones()
                                        .get(nombreAccion)
                                        .metodo()
                                        .apply(medicoId, params);

                                System.out.println("📦 Resultado acción " + nombreAccion + ": " + resultadoAccion);
                                seEjecutoAccion = true;

                                // Añadir mensaje de la acción a la respuesta
                                if (resultadoAccion != null && resultadoAccion.containsKey("mensaje")) {
                                    contenido += "\n\n" + resultadoAccion.get("mensaje");
                                }

                                // Guardar ejecución de acción en historial
                                contextService.guardar(
                                        medicoId,
                                        "[IA ejecutó acción: " + nombreAccion + "]",
                                        String.valueOf(resultadoAccion),
                                        dataFusionada
                                );

                            } else {
                                System.out.println("⚠️ Acción desconocida sugerida por IA: " + nombreAccion);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("❌ Error ejecutando acción IA (médico): " + ex.getMessage());
                ex.printStackTrace();
            }

            // 🧩 Si hubo alguna acción, pedimos a la IA una respuesta natural posterior
            if (seEjecutoAccion) {
                try {
                    String nuevosDatosJson = mapper.writeValueAsString(dataFusionada);

                    // Prompt post-acción
                    String promptReflexion = promptBuilderMedico.construirPromptReflexion(nuevosDatosJson, mensaje);

                    String respuestaReflexion = deepSeekService.generarTexto(promptReflexion);
                    JsonNode rootReflexion = mapper.readTree(respuestaReflexion);
                    String respuestaNatural = rootReflexion.path("choices").get(0).path("message").path("content").asText();

                    contenido += "\n\n" + respuestaNatural;
                    System.out.println("💬 [IA post-acción] Respuesta natural:\n" + respuestaNatural);

                    // Guardar reflexión natural
                    contextService.guardar(
                            medicoId,
                            "[IA reflexión natural]",
                            respuestaNatural,
                            dataFusionada
                    );

                } catch (Exception ex) {
                    System.err.println("⚠️ No se pudo generar respuesta natural post-acción: " + ex.getMessage());
                }
            }

            // ✅ Respuesta consolidada
            return Map.of(
                    "mensaje", contenido,
                    "data", dataFusionada
            );

        } catch (Exception e) {
            System.err.println("💥 Error en flujo de conversación médico: " + e.getMessage());
            e.printStackTrace();

            return Map.of(
                    "error", "Error en conversación médico",
                    "detalle", e.getMessage()
            );
        }
    }
}