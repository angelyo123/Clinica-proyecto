package com.AutomatizacionService.service.conversacionIA;

import com.AutomatizacionService.service.orquestador.DeepSeekService;
import com.AutomatizacionService.service.orquestador.SugerenciaCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConversacionPacienteService {

    @Autowired private DeepSeekService deepSeekService;
    @Autowired private SugerenciaCacheService cache;
    @Autowired private AccionIARegistry accionRegistry;
    @Autowired private AccionHandlers handlers;

    @Autowired private ConversacionPromptBuilder promptBuilder;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Registro automático de acciones disponibles.
     */
    @PostConstruct
    public void registrarAcciones() {
        accionRegistry.registrarAccion(
                "listar_medicos",
                "Devuelve la lista de médicos disponibles y sus especialidades.",
                handlers::listarMedicos
        );

        accionRegistry.registrarAccion(
                "consultar_horarios",
                "Obtiene los horarios disponibles para un médico o especialidad.",
                handlers::consultarHorarios
        );

        accionRegistry.registrarAccion(
                "crear_cita",
                "Crea una cita médica con la información del paciente, médico y horario.",
                handlers::crearCita
        );

        accionRegistry.registrarAccion(
                "cancelar_cita",
                "Cancela una cita médica existente.",
                handlers::cancelarCita
        );

        accionRegistry.registrarAccion(
                "saludo",
                "Saluda cordialmente al paciente.",
                (id, params) -> Map.of("mensaje", "¡Hola! Soy tu asistente médico. 😊")
        );
    }

    /**
     * Núcleo principal del sistema conversacional.
     */
    public Map<String, Object> procesarConversacion(Map<String, Object> solicitud) {
        try {
            Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());
            String mensaje = solicitud.get("mensaje").toString();

            String prompt = promptBuilder.construirPrompt(mensaje);


            // 2️⃣ Llamar a la IA DeepSeek
            String respuestaIA = deepSeekService.generarTexto(prompt);
            JsonNode root = mapper.readTree(respuestaIA);
            String contenido = root.path("choices").get(0)
                    .path("message").path("content").asText();

            Map<String, Object> decision = mapper.readValue(contenido, Map.class);
            String accion = decision.getOrDefault("accion", "").toString();

            // 3️⃣ Guardar contexto de conversación
            cache.agregarContextoConversacion(pacienteId, "Paciente: " + mensaje);
            cache.agregarContextoConversacion(pacienteId, "IA: " + decision.getOrDefault("respuesta", ""));

            // 4️⃣ Si no hay acción → respuesta libre
            if (accion.isBlank()) {
                return Map.of("mensaje",
                        decision.getOrDefault("respuesta", "No entendí tu mensaje. ¿Podrías repetirlo?"));
            }

            // 5️⃣ Buscar y ejecutar acción dinámica
            var accionIA = accionRegistry.getAcciones().get(accion);
            if (accionIA != null) {
                System.out.println("🧠 Ejecutando acción dinámica: " + accionIA.nombre());
                return accionIA.metodo().apply(pacienteId, decision);
            }

            // 6️⃣ Fallback
            return Map.of("mensaje", decision.getOrDefault("respuesta",
                    "Disculpa, no tengo una acción registrada para eso aún."));

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error en conversación", "detalle", e.getMessage());
        }
    }
}
