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

            // 🔄 Verificar actualización de médicos (separado)
            actualizacionMedicosService.verificarYActualizar(pacienteId);

            // 🧠 Construir contexto
            String contextoPrevio = contextService.construirContextoConversacion(pacienteId);
            String ultimoJson = contextService.obtenerUltimoData(pacienteId);
            String memoria = contextService.construirMemoriaPaciente(pacienteId);

            // 🧩 Construir prompt principal
            String prompt = promptBuilder.construirPrompt(mensaje, contextoPrevio, ultimoJson, memoria);

            // 🔮 Llamar a DeepSeek
            String respuestaIA = deepSeekService.generarTexto(prompt);
            JsonNode root = mapper.readTree(respuestaIA);
            String contenido = root.path("choices").get(0).path("message").path("content").asText();

            Map<String, Object> decision;
            try {
                decision = mapper.readValue(contenido, Map.class);
            } catch (Exception e) {
                decision = Map.of("accion", "", "parametros", Map.of(), "respuesta", contenido);
            }

            // 💾 Guardar conversación
            contextService.guardar(pacienteId, mensaje,
                    decision.getOrDefault("respuesta", "").toString(), decision.get("data"));

            String accion = decision.getOrDefault("accion", "").toString();
            if (accion.isBlank()) {
                return Map.of("mensaje", decision.getOrDefault("respuesta", "No entendí tu mensaje."));
            }

            // ⚙️ Ejecutar acción
            var accionIA = accionRegistry.getAcciones().get(accion);
            if (accionIA == null) {
                return Map.of("mensaje", "Disculpa, no tengo una acción registrada para eso aún.");
            }

            System.out.println("🧠 Ejecutando acción dinámica: " + accionIA.nombre());
            Map<String, Object> resultadoAccion = accionIA.metodo().apply(pacienteId, decision);

            // 💾 Guardar nueva data
            contextService.guardar(pacienteId, mensaje,
                    resultadoAccion.getOrDefault("mensaje", "").toString(),
                    resultadoAccion.get("data"));

            // 🤔 Si hay data, generar razonamiento automático
            if (resultadoAccion.get("data") != null) {
                return razonamientoIAService.generarRazonamientoConDatos(
                        pacienteId, resultadoAccion.get("data"), memoria);
            }

            return resultadoAccion;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error en conversación", "detalle", e.getMessage());
        }
    }
}
