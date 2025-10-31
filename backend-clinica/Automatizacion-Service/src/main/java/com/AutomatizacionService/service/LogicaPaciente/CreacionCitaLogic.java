package com.AutomatizacionService.service.LogicaPaciente;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.model.CitaRequest;
import com.AutomatizacionService.model.SugerenciaPendiente;
import com.AutomatizacionService.repository.SugerenciaPendienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    /**
     * 🧠 Crea una cita médica a partir de la decisión generada por la IA.
     * La IA ya devuelve el id del médico correcto según la lista de especialidades reales.
     */
    public Map<String, Object> crearCitaDesdeDecision(Map<String, Object> decision, Long pacienteId,
                                                      List<Map<String, Object>> medicos) {
        try {
            // Datos que devuelve la IA
            String especialidad = decision.getOrDefault("especialidad", "Medicina General").toString();
            String fecha = decision.getOrDefault("fecha", java.time.LocalDate.now().plusDays(1).toString()).toString();
            String hora = decision.getOrDefault("hora", "09:00").toString();
            Long medicoId = Long.valueOf(decision.getOrDefault("medicoId", "0").toString());

            // Validaciones básicas
            if (medicoId == 0) {
                throw new IllegalArgumentException("❌ La IA no devolvió un ID de médico válido.");
            }

            // --- Validar hora --- //
            if (hora.equalsIgnoreCase("HH:mm") || !hora.matches("^\\d{2}:\\d{2}$")) {
                System.out.println("⚠️ [IA] Hora no válida detectada, se asignará valor por defecto 09:00");
                hora = "09:00";
            }

// --- Construir fecha-hora en formato ISO --- //
            String fechaHoraTexto = fecha + "T" + hora + ":00"; // ejemplo: 2025-10-30T09:00:00
            LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraTexto, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            System.out.println("🩺 [IA] Asignando cita automáticamente...");
            System.out.println("📋 Especialidad: " + especialidad);
            System.out.println("👨‍⚕️ Médico ID: " + medicoId);
            System.out.println("📅 FechaHora: " + fechaHora);

            // Crear cita en el microservicio de citas
            CitaRequest cita = new CitaRequest(pacienteId, medicoId, fechaHora);
            citaClient.crearCita(cita);

            // Registrar sugerencia de la IA
            sugerenciaPendienteRepository.save(SugerenciaPendiente.builder()
                    .pacienteId(pacienteId)
                    .medicoId(medicoId)
                    .fecha(fecha)
                    .hora(hora)
                    .mensaje("Cita creada automáticamente por IA")
                    .confirmada(true)
                    .build());

            System.out.println("✅ [OK] Cita creada correctamente por IA para paciente " + pacienteId);
            return Map.of(
                    "mensaje", "Cita creada automáticamente",
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
}
