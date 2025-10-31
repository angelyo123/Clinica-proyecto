package com.AutomatizacionService.service.LogicaPaciente;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.model.dto.CitaDecisionDTO;
import com.AutomatizacionService.model.dto.CitaRequest;
import com.AutomatizacionService.model.entity.SugerenciaPendiente;
import com.AutomatizacionService.repository.SugerenciaPendienteRepository;
import com.AutomatizacionService.service.orquestador.DeepSeekService;
import com.AutomatizacionService.service.orquestador.SugerenciaCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
@Service
public class ConfirmacionCitaLogic {

    @Autowired private DeepSeekService deepSeekService;
    @Autowired private CitaClient citaClient;
    @Autowired private SugerenciaCacheService cache;
    @Autowired private SugerenciaPendienteRepository repo;

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> procesarConfirmacion(CitaDecisionDTO solicitud) {
        try {
            Long pacienteId = solicitud.getPacienteId();
            String mensaje = solicitud.getMensaje() != null ? solicitud.getMensaje() : "";

            System.out.println("🤖 [IA] Analizando confirmación del paciente: " + mensaje);

            // 🧠 1. Pregunta a DeepSeek si confirma o cancela
            String prompt = """
Eres un asistente médico. Interpreta si el paciente acepta o rechaza una cita.
Devuelve SOLO un JSON:
{
  "intencion": "confirmar_cita" | "rechazar_cita",
  "razon": "<opcional>"
}
Texto del paciente: "%s"
""".formatted(mensaje);

            String respuestaIA = deepSeekService.generarTexto(prompt);
            System.out.println("🧠 Respuesta IA (confirmación): " + respuestaIA);

            // 🧩 2. Extraer contenido JSON de DeepSeek
            JsonNode root = mapper.readTree(respuestaIA);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            Map<String, Object> decision = mapper.readValue(content, Map.class);

            String intencion = decision.getOrDefault("intencion", "").toString();

            // 🧩 3. Buscar sugerencia previa
            Map<String, Object> sugerencia = cache.obtenerSugerencia(pacienteId);
            if (sugerencia == null) {
                SugerenciaPendiente pendiente = repo.findTopByPacienteIdAndConfirmadaFalseOrderByIdDesc(pacienteId)
                        .orElse(null);
                if (pendiente != null) {
                    sugerencia = Map.of(
                            "medicoId", pendiente.getMedicoId(),
                            "fecha", pendiente.getFecha(),
                            "hora", pendiente.getHora(),
                            "especialidad", pendiente.getEspecialidad()
                    );
                }
            }

            if (sugerencia == null) {
                return Map.of("mensaje", "⚠️ No hay ninguna cita pendiente para confirmar o cancelar.");
            }

            // 🩺 4. Procesar intención
            if ("confirmar_cita".equalsIgnoreCase(intencion)) {
                // Construir LocalDateTime a partir de la fecha y hora separadas
                String fecha = sugerencia.get("fecha").toString();
                String hora = sugerencia.get("hora").toString();

                // Valida que no vengan vacíos
                if (fecha.isEmpty() || hora.isEmpty()) {
                    return Map.of("mensaje", "⚠️ Faltan datos de fecha u hora para crear la cita.");
                }

                LocalDateTime fechaHora = LocalDateTime.parse(
                        fecha + "T" + hora,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                );

                // ✅ Construir DTO CitaRequest (usa nombres exactos de tus campos)
                CitaRequest citaRequest = new CitaRequest(
                        pacienteId,
                        Long.valueOf(sugerencia.get("medicoId").toString()),
                        fechaHora
                );

                System.out.println("📦 Enviando CitaRequest: " + citaRequest);

                // ✅ Llamar al microservicio CitaService
                Map<String, Object> citaCreada = citaClient.crearCita(citaRequest);

                // Marcar sugerencia como confirmada
                repo.findTopByPacienteIdAndConfirmadaFalseOrderByIdDesc(pacienteId)
                        .ifPresent(p -> {
                            p.setConfirmada(true);
                            repo.save(p);
                        });

                cache.eliminarSugerencia(pacienteId);

                return Map.of(
                        "mensaje", "✅ Tu cita ha sido confirmada correctamente.",
                        "cita", citaCreada
                );
            } else if ("rechazar_cita".equalsIgnoreCase(intencion)) {
                repo.findTopByPacienteIdAndConfirmadaFalseOrderByIdDesc(pacienteId)
                        .ifPresent(p -> {
                            p.setConfirmada(true); // marcar como atendida
                            repo.save(p);
                        });
                cache.eliminarSugerencia(pacienteId);

                return Map.of(
                        "mensaje", "❌ Se ha cancelado la propuesta de cita. No se ha creado ninguna cita médica."
                );
            }

            return Map.of("mensaje", "🤔 No se detectó respuesta clara del paciente.");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error al procesar confirmación", "detalle", e.getMessage());
        }
    }
}