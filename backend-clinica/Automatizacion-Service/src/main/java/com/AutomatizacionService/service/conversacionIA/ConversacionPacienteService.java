package com.AutomatizacionService.service.conversacionIA;

import com.AutomatizacionService.service.conversacionIA.context.ConversacionContextService;
import com.AutomatizacionService.service.orquestador.DeepSeekService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConversacionPacienteService {

    @Autowired private DeepSeekService deepSeekService;
    @Autowired private AccionIARegistry accionRegistry;
    @Autowired private ConversacionContextService contextService;
    @Autowired private ConversacionPromptBuilder promptBuilder;
    @Autowired private com.AutomatizacionService.service.LogicaMedico.ListarMedicosService listarMedicosService;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void registrarAcciones() {
        accionRegistry.registrarAccion("listar_medicos",
                "Devuelve la lista de médicos disponibles y sus especialidades.",
                listarMedicosService::listarMedicos);

        accionRegistry.registrarAccion("saludo",
                "Saluda cordialmente al paciente.",
                (id, params) -> Map.of("mensaje", "¡Hola! Soy tu asistente médico. 😊"));
    }

    /**
     * Núcleo principal del sistema conversacional.
     */
    public Map<String, Object> procesarConversacion(Map<String, Object> solicitud) {
        try {
            Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());
            String mensaje = solicitud.get("mensaje").toString();

            // 🔄 Consultar lista de médicos (solo se actualizará si hubo cambios)

            Map<String, Object> medicosResponse = listarMedicosService.listarMedicos(pacienteId, Map.of());
            List<Map<String, Object>> listaActual = (List<Map<String, Object>>) medicosResponse.get("data");

// 🧠 Si hubo actualización, guardamos y regeneramos contexto inmediatamente
            if (listaActual != null && !listaActual.isEmpty()) {
                contextService.guardar(pacienteId, "Sistema (médicos actualizados)",
                        "Lista médica sincronizada con el microservicio.", listaActual);

                // 🧩 Recalcular contexto y memoria con la nueva data
                String nuevoContexto = contextService.construirContextoConversacion(pacienteId);
                String nuevaMemoria = contextService.construirMemoriaPaciente(pacienteId);

                // ⚡ Generar razonamiento inmediato sobre la lista actualizada
                System.out.println("⚡ Razonando inmediatamente tras actualización de médicos...");
                String promptAuto = promptBuilder.construirPrompt(
                        "Analiza la lista actualizada de médicos y resume su disponibilidad al paciente.",
                        nuevoContexto,
                        mapper.writeValueAsString(listaActual),
                        nuevaMemoria
                );

                try {
                    String razonamientoIA = deepSeekService.generarTexto(promptAuto);
                    String razonado = mapper.readTree(razonamientoIA)
                            .path("choices").get(0).path("message").path("content").asText();
                    contextService.guardar(pacienteId, "Sistema (razonamiento inmediato)", razonado, listaActual);
                } catch (Exception ex) {
                    System.err.println("⚠️ Error generando razonamiento inmediato: " + ex.getMessage());
                }
            }


            // 🧠 Construir contexto y memoria
            String contextoPrevio = contextService.construirContextoConversacion(pacienteId);
            String ultimoJson = contextService.obtenerUltimoData(pacienteId);
            String memoria = contextService.construirMemoriaPaciente(pacienteId);

            // 🧩 Construir prompt completo
            String prompt = promptBuilder.construirPrompt(mensaje, contextoPrevio, ultimoJson, memoria);

            // 🔮 Llamar a DeepSeek para interpretar la intención del paciente
            String respuestaIA = deepSeekService.generarTexto(prompt);
            JsonNode root = mapper.readTree(respuestaIA);
            String contenido = root.path("choices").get(0).path("message").path("content").asText();

            Map<String, Object> decision;
            try {
                decision = mapper.readValue(contenido, Map.class);
            } catch (Exception e) {
                decision = Map.of("accion", "", "parametros", Map.of(), "respuesta", contenido);
            }

            String accion = decision.getOrDefault("accion", "").toString();

            // Guardar conversación inicial
            contextService.guardar(pacienteId, mensaje,
                    decision.getOrDefault("respuesta", "").toString(), decision.get("data"));

            // Si no hay acción, responder directamente
            if (accion.isBlank()) {
                return Map.of("mensaje",
                        decision.getOrDefault("respuesta", "No entendí tu mensaje. ¿Podrías repetirlo?"));
            }

            // Ejecutar acción registrada
            var accionIA = accionRegistry.getAcciones().get(accion);
            if (accionIA == null) {
                return Map.of("mensaje", "Disculpa, no tengo una acción registrada para eso aún.");
            }

            System.out.println("🧠 Ejecutando acción dinámica: " + accionIA.nombre());
            Map<String, Object> resultadoAccion = accionIA.metodo().apply(pacienteId, decision);

            // Guardar datos obtenidos
            contextService.guardar(pacienteId, mensaje,
                    resultadoAccion.getOrDefault("mensaje", "").toString(),
                    resultadoAccion.get("data"));

            // 🧠 Generar razonamiento con los datos reales
            if (resultadoAccion.get("data") != null) {
                System.out.println("📡 Razonando con lista médica actual...");
                String promptRazonar = promptBuilder.construirPrompt(
                        "Analiza la información médica más reciente y responde al paciente naturalmente.",
                        "", // sin contexto viejo
                        mapper.writeValueAsString(resultadoAccion.get("data")),
                        memoria
                );

                String respuestaRazonada = deepSeekService.generarTexto(promptRazonar);
                String contenidoRazonado = mapper.readTree(respuestaRazonada)
                        .path("choices").get(0)
                        .path("message").path("content").asText();

                contextService.guardar(pacienteId,
                        "Sistema (razonamiento automático)", contenidoRazonado, null);

                return Map.of("mensaje", contenidoRazonado, "data", resultadoAccion.get("data"));
            }

            // Si no hay data, responder con el resultado base
            return resultadoAccion;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error en conversación", "detalle", e.getMessage());
        }
    }
}
