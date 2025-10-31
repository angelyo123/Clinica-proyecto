package com.AutomatizacionService.service.LogicaPaciente;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.model.dto.CitaDecisionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CancelacionCitaLogic {

    @Autowired
    private CitaClient citaClient;

    public Map<String, Object> procesarCancelacion(CitaDecisionDTO solicitud) {
        try {
            // 📌 Verificamos primero si viene un citaId o pacienteId en el DTO
            Long pacienteId = solicitud.getPacienteId();
            Long medicoId = solicitud.getMedicoId(); // opcional si lo necesitas
            String mensaje = solicitud.getMensaje() != null ? solicitud.getMensaje() : "";

            // 🔍 Si viene un pacienteId, cancelar todas sus citas activas
            if (pacienteId != null && pacienteId > 0) {
                citaClient.cancelarPorPaciente(pacienteId);
                return Map.of(
                        "mensaje", "🩺 Todas las citas activas del paciente " + pacienteId + " han sido canceladas correctamente.",
                        "accion", "cancelar_cita",
                        "detalle", mensaje
                );
            }

            // 🔍 Si viene una citaId (en caso de extensión futura)
            // Puedes agregar este campo al DTO si lo necesitas más adelante.
            // Ejemplo:
            // if (solicitud.getCitaId() != null) { ... }

            // ⚠️ Si no hay identificador, error controlado
            return Map.of(
                    "error", "Debe especificar un pacienteId válido para cancelar sus citas."
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "error", "⚠️ Error al cancelar cita",
                    "detalle", e.getMessage()
            );
        }
    }
}