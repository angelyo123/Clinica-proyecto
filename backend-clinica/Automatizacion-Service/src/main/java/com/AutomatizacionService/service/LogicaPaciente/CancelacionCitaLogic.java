package com.AutomatizacionService.service.LogicaPaciente;

import com.AutomatizacionService.client.CitaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CancelacionCitaLogic {

    @Autowired
    private CitaClient citaClient;

    public Map<String, Object> procesarCancelacion(Map<String, Object> solicitud) {
        try {
            if (solicitud.containsKey("pacienteId")) {
                Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());
                citaClient.cancelarPorPaciente(pacienteId);
                return Map.of("mensaje", "🩺 Citas del paciente canceladas correctamente.");
            } else if (solicitud.containsKey("citaId")) {
                Long citaId = Long.valueOf(solicitud.get("citaId").toString());
                citaClient.cancelarCita(citaId);
                return Map.of("mensaje", "🗓️ Cita cancelada correctamente.");
            } else {
                return Map.of("error", "Debe especificar pacienteId o citaId.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }
}