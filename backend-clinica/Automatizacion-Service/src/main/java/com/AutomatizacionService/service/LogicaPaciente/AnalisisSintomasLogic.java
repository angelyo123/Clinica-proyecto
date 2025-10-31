package com.AutomatizacionService.service.LogicaPaciente;

import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.model.dto.CitaDecisionDTO;
import com.AutomatizacionService.model.entity.SugerenciaPendiente;
import com.AutomatizacionService.repository.SugerenciaPendienteRepository;
import com.AutomatizacionService.service.orquestador.SugerenciaCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AnalisisSintomasLogic {

    @Autowired private CreacionCitaLogic creacionCitaLogic;
    @Autowired private ConfirmacionCitaLogic confirmacionCitaLogic;
    @Autowired private CancelacionCitaLogic cancelacionCitaLogic;
    @Autowired private SugerenciaCacheService cache;
    @Autowired private SugerenciaPendienteRepository repo;
    @Autowired private HorarioClient horarioClient;

    /**
     * 🧠 Procesa el mensaje del paciente (ya analizado por la IA)
     * y actúa según la intención: crear, confirmar o cancelar.
     */
    public Map<String, Object> procesarMensajePaciente(CitaDecisionDTO solicitud) {
        try {
            Long pacienteId = solicitud.getPacienteId();
            String mensaje = solicitud.getMensaje() != null ? solicitud.getMensaje() : "";
            String especialidad = solicitud.getEspecialidad() != null ? solicitud.getEspecialidad() : "";
            String fecha = solicitud.getFecha() != null ? solicitud.getFecha() : "";
            String hora = solicitud.getHora() != null ? solicitud.getHora() : "";
            Long medicoId = solicitud.getMedicoId();

            System.out.println("🤖 [IA] Analizando mensaje del paciente: " + mensaje);

            // 🧩 1️⃣ Reforzar datos faltantes desde contexto
            if (especialidad.isBlank()) {
                Object ctxEsp = cache.obtenerDatoContexto(pacienteId, "especialidad");
                if (ctxEsp != null) {
                    especialidad = ctxEsp.toString();
                    System.out.println("🔄 Reforzando especialidad desde contexto: " + especialidad);
                }
            }

            if (medicoId == null || medicoId == 0) {
                Object ctxMed = cache.obtenerDatoContexto(pacienteId, "medicoId");
                if (ctxMed instanceof Number num) {
                    medicoId = num.longValue();
                    System.out.println("🔄 Reforzando medicoId desde contexto: " + medicoId);
                }
            }

            // 🕒 Si no hay fecha → sugerir próxima disponible
            if (fecha.isBlank()) {
                try {
                    List<Map<String, Object>> horarios = horarioClient.listarPorMedico(medicoId);
                    System.out.println("📅 Horarios obtenidos desde horario-service: " +
                            (horarios != null ? horarios.size() : 0));

                    if (horarios == null) {
                        return Map.of(
                                "mensaje", "⚠️ No se pudieron obtener los horarios del médico seleccionado.",
                                "propuesta", false
                        );
                    }

                    if (horarios.isEmpty()) {
                        return Map.of(
                                "mensaje", "😕 El médico no tiene horarios configurados por ahora.",
                                "propuesta", false
                        );
                    }

                    // ✅ Elegir el primer horario disponible
                    Map<String, Object> horarioDisponible = horarios.stream()
                            .filter(h -> Boolean.TRUE.equals(h.get("disponible")))
                            .findFirst()
                            .orElse(horarios.get(0));

                    String diaSemana = horarioDisponible.get("diaSemana").toString();
                    fecha = obtenerProximaFecha(diaSemana);
                    hora = horarioDisponible.get("horaInicio").toString();

                    System.out.println("🧭 Fecha sugerida por horario: " + fecha + " a las " + hora);

                } catch (Exception e) {
                    System.err.println("❌ Error al consultar horarios del médico: " + e.getMessage());
                    return Map.of(
                            "mensaje", "⚠️ No se pudieron obtener los horarios de la especialidad " + especialidad + ".",
                            "propuesta", false
                    );
                }
            }

            // 🧩 2️⃣ Decidir acción según intención
            String intencion = mensaje.toLowerCase();

            if (intencion.contains("crear") || intencion.contains("agendar") || intencion.contains("cita")) {
                // ✅ Crear cita
                repo.save(SugerenciaPendiente.builder()
                        .pacienteId(pacienteId)
                        .medicoId(medicoId)
                        .fecha(fecha)
                        .hora(hora)
                        .especialidad(especialidad)
                        .mensaje("Propuesta de cita: " + mensaje)
                        .confirmada(false)
                        .build());

                return creacionCitaLogic.crearCitaDesdeDecision(
                        Map.of(
                                "especialidad", especialidad,
                                "fecha", fecha,
                                "hora", hora,
                                "medicoId", medicoId
                        ),
                        pacienteId,
                        null
                );
            }

            if (intencion.contains("confirmar")) {
                return confirmacionCitaLogic.procesarConfirmacion(solicitud);
            }

            if (intencion.contains("cancelar") || intencion.contains("anular")) {
                return cancelacionCitaLogic.procesarCancelacion(solicitud);
            }

            // 🧩 3️⃣ Por defecto
            return Map.of(
                    "mensaje", "🤔 No entendí tu solicitud. ¿Deseas crear, confirmar o cancelar una cita?"
            );

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

        return hoy.plusDays(diasHastaObjetivo).toString();
    }
}