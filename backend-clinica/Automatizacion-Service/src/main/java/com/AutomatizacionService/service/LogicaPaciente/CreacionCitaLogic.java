package com.AutomatizacionService.service.LogicaPaciente;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.model.CitaRequest;
import com.AutomatizacionService.model.SugerenciaPendiente;
import com.AutomatizacionService.repository.SugerenciaPendienteRepository;
import com.AutomatizacionService.service.SugerenciaCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class CreacionCitaLogic {

    @Autowired
    private CitaClient citaClient;

    @Autowired
    private SugerenciaPendienteRepository sugerenciaPendienteRepository;

    @Autowired
    private SugerenciaCacheService cache;

    /**
     * 🧠 Crea una cita médica a partir de la decisión generada por la IA.
     * Asegura valores válidos y recupera datos faltantes desde contexto.
     */
    public Map<String, Object> crearCitaDesdeDecision(Map<String, Object> decision, Long pacienteId,
                                                      List<Map<String, Object>> medicos) {
        try {
            // 📦 Extraer campos base
            String especialidad = safeString(decision.get("especialidad"), "Medicina General");
            String fecha = safeString(decision.get("fecha"), LocalDate.now().plusDays(1).toString());
            String hora = safeString(decision.get("hora"), "09:00");

            // 📌 Recuperar medicoId de forma segura
            Long medicoId = safeLong(decision.get("medicoId"));
            if (medicoId == null || medicoId == 0) {
                Object ctxMed = cache.obtenerDatoContexto(pacienteId, "medicoId");
                if (ctxMed instanceof Number num) {
                    medicoId = num.longValue();
                    System.out.println("🔄 Reforzando medicoId desde contexto: " + medicoId);
                } else {
                    throw new IllegalArgumentException("❌ No se pudo determinar el médico para la cita.");
                }
            }

            // 📌 Recuperar especialidad si está vacía
            if (especialidad.isBlank()) {
                Object ctxEsp = cache.obtenerDatoContexto(pacienteId, "especialidad");
                if (ctxEsp != null) {
                    especialidad = ctxEsp.toString();
                    System.out.println("🔄 Reforzando especialidad desde contexto: " + especialidad);
                }
            }

            // --- Validar hora --- //
            if (hora.equalsIgnoreCase("HH:mm") || !hora.matches("^\\d{2}:\\d{2}$")) {
                System.out.println("⚠️ [IA] Hora no válida detectada, se asignará valor por defecto 09:00");
                hora = "09:00";
            }

            // --- Construir fecha-hora en formato ISO --- //
            String fechaHoraTexto = fecha + "T" + hora + ":00";
            LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraTexto, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            System.out.println("🩺 [IA] Asignando cita automáticamente...");
            System.out.println("📋 Especialidad: " + especialidad);
            System.out.println("👨‍⚕️ Médico ID: " + medicoId);
            System.out.println("📅 FechaHora: " + fechaHora);

            // ✅ Crear cita en microservicio
            CitaRequest cita = new CitaRequest(pacienteId, medicoId, fechaHora);
            citaClient.crearCita(cita);

            // 💾 Guardar sugerencia
            sugerenciaPendienteRepository.save(SugerenciaPendiente.builder()
                    .pacienteId(pacienteId)
                    .medicoId(medicoId)
                    .fecha(fecha)
                    .hora(hora)
                    .especialidad(especialidad)
                    .mensaje("Cita creada automáticamente por IA")
                    .confirmada(true)
                    .build());

            System.out.println("✅ [OK] Cita creada correctamente para paciente " + pacienteId);
            return Map.of(
                    "mensaje", "✅ Cita creada con éxito para el " + fecha + " a las " + hora +
                            " con el especialista en " + especialidad + ".",
                    "especialidad", especialidad,
                    "medicoId", medicoId,
                    "fecha", fecha,
                    "hora", hora
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "error", "⚠️ No se pudo crear la cita automáticamente.",
                    "detalle", e.getMessage()
            );
        }
    }

    // ====== 🔧 Helpers ======

    private String safeString(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        String s = value.toString().trim();
        return s.isEmpty() ? defaultValue : s;
    }

    private Long safeLong(Object value) {
        try {
            if (value == null) return null;
            if (value instanceof Number num) return num.longValue();
            String str = value.toString().trim();
            if (str.isEmpty()) return null;
            return Long.parseLong(str);
        } catch (Exception e) {
            return null;
        }
    }
}