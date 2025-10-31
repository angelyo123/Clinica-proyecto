package com.AutomatizacionService.service.LogicaPaciente;

import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.client.MedicoClient;
import com.AutomatizacionService.client.PacienteClient;
import com.AutomatizacionService.model.SugerenciaPendiente;
import com.AutomatizacionService.repository.SugerenciaPendienteRepository;
import com.AutomatizacionService.service.DeepSeekService;
import com.AutomatizacionService.service.SugerenciaCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AnalisisSintomasLogic {

    @Autowired private DeepSeekService deepSeekService;
    @Autowired private CreacionCitaLogic creacionCitaLogic;
    @Autowired private ConfirmacionCitaLogic confirmacionCitaLogic;
    @Autowired private CancelacionCitaLogic cancelacionCitaLogic;
    @Autowired private SugerenciaCacheService cache;
    @Autowired private SugerenciaPendienteRepository repo;
    @Autowired private HorarioClient horarioClient;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 🧠 Procesa el mensaje libre del paciente y decide qué hacer según la IA de DeepSeek.
     */
    public Map<String, Object> procesarMensajePaciente(Map<String, Object> solicitud) {
        try {
            Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());
            String mensaje = solicitud.get("mensaje").toString();

            System.out.println("🤖 [IA] Analizando mensaje del paciente: " + mensaje);

            // 🧩 1. Enviar texto a DeepSeek
            String respuestaIA = deepSeekService.generarTexto("""
    Eres un asistente médico digital que ayuda a pacientes a gestionar sus citas médicas.
    Tu tarea es ANALIZAR el mensaje del paciente y devolver un JSON con la intención detectada.
    
    Debes identificar:
    - Si el paciente quiere CREAR, CONFIRMAR o CANCELAR una cita.
    - La ESPECIALIDAD médica (por ejemplo: "Cardiología", "Dermatología", "Neurología", etc.).
    - La FECHA y HORA, si las menciona (convierte a formato ISO: YYYY-MM-DD y HH:mm en 24h).
    - El ID del médico (usa 1 si no tienes datos reales).
    
    Ejemplos:
    1️⃣ "quiero una cita con un cardiólogo mañana a las 9am" =>
        {
          "intencion": "crear_cita",
          "especialidad": "Cardiología",
          "fecha": "2025-10-31",
          "hora": "09:00",
          "medicoId": 1
        }
    2️⃣ "sí, confirmo mi cita con el dermatólogo" =>
        {
          "intencion": "confirmar_cita"
        }
    3️⃣ "no podré asistir a mi cita" =>
        {
          "intencion": "cancelar_cita"
        }

    ⚠️ IMPORTANTE:
    - Devuelve SOLO JSON (sin texto extra).
    - Si no puedes identificar algo, deja el campo vacío.
    
    Mensaje del paciente: "%s"
    """.formatted(mensaje));

            System.out.println("🧠 Respuesta IA: " + respuestaIA);

            JsonNode root = mapper.readTree(respuestaIA);
            String contenido = root.path("choices").get(0).path("message").path("content").asText();
            Map<String, Object> decision = mapper.readValue(contenido, Map.class);
            String intencion = decision.getOrDefault("intencion", "").toString().toLowerCase();



            String fecha = decision.getOrDefault("fecha", "").toString();
            String hora = decision.getOrDefault("hora", "").toString();
            Long medicoId = Long.valueOf(decision.get("medicoId").toString());

// 🕒 Si la IA no detectó una fecha, sugerir una en base a horarios disponibles
            if (fecha.isEmpty()) {
                try {
                    List<Map<String, Object>> horarios = horarioClient.listarPorMedico(medicoId);
                    System.out.println("📅 Horarios obtenidos desde horario-service: " + horarios.size());

                    if (horarios == null || horarios.isEmpty()) {
                        // 🚨 Sin horarios registrados para ese médico
                        return Map.of(
                                "mensaje", "😕 Lo siento, no hay horarios disponibles con la especialidad de "
                                        + decision.get("especialidad") + ".",
                                "propuesta", false,
                                "decisionIA", decision
                        );
                    }

                    // Filtrar los disponibles
                    Map<String, Object> horarioDisponible = horarios.stream()
                            .filter(h -> Boolean.TRUE.equals(h.get("disponible")))
                            .findFirst()
                            .orElse(null);

                    if (horarioDisponible == null) {
                        // 🚫 Ningún horario activo
                        return Map.of(
                                "mensaje", "😕 Actualmente no hay citas disponibles para "
                                        + decision.get("especialidad") + ". Inténtalo más tarde.",
                                "propuesta", false,
                                "decisionIA", decision
                        );
                    }

                    // ✅ Hay horario disponible → sugerir fecha
                    String diaSemana = horarioDisponible.get("diaSemana").toString();
                    fecha = obtenerProximaFecha(diaSemana);
                    hora = horarioDisponible.get("horaInicio").toString();

                    System.out.println("🧭 Fecha sugerida por horario: " + fecha + " a las " + hora);

                } catch (Exception e) {
                    System.err.println("❌ Error al consultar horarios del médico: " + e.getMessage());
                    return Map.of(
                            "mensaje", "⚠️ No se pudieron obtener los horarios de la especialidad "
                                    + decision.get("especialidad") + ".",
                            "propuesta", false,
                            "decisionIA", decision
                    );
                }
            }



            // 🧩 3. Redirigir según intención
            switch (intencion) {
                case "crear_cita" -> {
                    // 🔄 Actualizar el mapa con la fecha/hora corregidas
                    decision.put("fecha", fecha);
                    decision.put("hora", hora);

                    // 💾 Guardar sugerencia temporal en Redis
                    cache.guardarSugerencia(pacienteId, decision);

                    // 🗂 Guardar en BD para persistencia
                    repo.save(SugerenciaPendiente.builder()
                            .pacienteId(pacienteId)
                            .medicoId(medicoId)
                            .fecha(fecha)
                            .hora(hora)
                            .especialidad(decision.get("especialidad").toString())
                            .mensaje("Propuesta de cita: " + mensaje)
                            .confirmada(false)
                            .build());

                    return Map.of(
                            "mensaje", "💬 Te propongo una cita con " + decision.get("especialidad") +
                                    " el " + fecha + " a las " + hora + ". ¿Deseas confirmarla?",
                            "propuesta", true,
                            "decisionIA", decision
                    );
                }


                case "confirmar_cita" -> {
                    return confirmacionCitaLogic.procesarConfirmacion(solicitud);
                }

                case "cancelar_cita" -> {
                    return cancelacionCitaLogic.procesarCancelacion(solicitud);
                }

                default -> {
                    return Map.of(
                            "mensaje", "🤔 No entendí tu solicitud. ¿Deseas crear, confirmar o cancelar una cita?",
                            "intencionDetectada", intencion
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "error", "⚠️ Error al procesar mensaje del paciente",
                    "detalle", e.getMessage()
            );
        }
    }

    private String obtenerProximaFecha(String diaSemana) {
        DayOfWeek objetivo = switch (diaSemana.toLowerCase()) {
            case "lunes" -> DayOfWeek.MONDAY;
            case "martes" -> DayOfWeek.TUESDAY;
            case "miércoles", "miercoles" -> DayOfWeek.WEDNESDAY;
            case "jueves" -> DayOfWeek.THURSDAY;
            case "viernes" -> DayOfWeek.FRIDAY;
            case "sábado", "sabado" -> DayOfWeek.SATURDAY;
            case "domingo" -> DayOfWeek.SUNDAY;
            default -> DayOfWeek.MONDAY;
        };

        LocalDate hoy = LocalDate.now();
        int diasHastaObjetivo = (objetivo.getValue() - hoy.getDayOfWeek().getValue() + 7) % 7;
        if (diasHastaObjetivo == 0) diasHastaObjetivo = 7; // siguiente semana si es el mismo día

        LocalDate proxima = hoy.plusDays(diasHastaObjetivo);
        return proxima.toString();
    }
}