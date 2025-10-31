package com.AutomatizacionService.service.orquestador;

import com.AutomatizacionService.model.dto.CitaDecisionDTO;
import com.AutomatizacionService.service.LogicaMedico.RegistroHorarioLogic;
import com.AutomatizacionService.service.LogicaPaciente.AnalisisSintomasLogic;
import com.AutomatizacionService.service.LogicaPaciente.CancelacionCitaLogic;
import com.AutomatizacionService.service.LogicaPaciente.ConfirmacionCitaLogic;
import com.AutomatizacionService.service.conversacionIA.ConversacionPacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SugerenciaIAService {

    @Autowired private AnalisisSintomasLogic analisisSintomasLogic;
    @Autowired private ConfirmacionCitaLogic confirmacionCitaLogic;
    @Autowired private CancelacionCitaLogic cancelacionCitaLogic;
    @Autowired private RegistroHorarioLogic registroHorarioLogic;
    @Autowired
    private ConversacionPacienteService conversacionPacienteService;

    public Map<String, Object> procesarConversacion(Map<String, Object> solicitud) {
        return conversacionPacienteService.procesarConversacion(solicitud);
    }


    // 🧠 PACIENTE
    public Map<String, Object> procesarPaciente(CitaDecisionDTO solicitud) {
        return wrapLogic(() -> analisisSintomasLogic.procesarMensajePaciente(solicitud), "AnalisisSintomasLogic");
    }

    // 👨‍⚕️ MÉDICO
    public Map<String, Object> procesarMedico(Map<String, Object> solicitud) {
        return wrapLogic(() -> registroHorarioLogic.procesarMensajeMedico(solicitud), "RegistroHorarioLogic");
    }

    // ✅ CONFIRMACIÓN
    public Map<String, Object> procesarConfirmacion(CitaDecisionDTO solicitud) {
        return wrapLogic(() -> confirmacionCitaLogic.procesarConfirmacion(solicitud), "ConfirmacionCitaLogic");
    }

    // ❌ CANCELACIÓN
    public Map<String, Object> procesarCancelacion(CitaDecisionDTO solicitud) {
        return wrapLogic(() -> cancelacionCitaLogic.procesarCancelacion(solicitud), "CancelacionCitaLogic");
    }

    /** Envoltorio genérico con mensajes de error amigables **/
    private Map<String, Object> wrapLogic(LogicExecutor executor, String logicName) {
        try {
            return executor.execute();
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "error", "❌ Error en lógica: " + logicName,
                    "detalle", e.getMessage(),
                    "ubicacion", logicName
            );
        }
    }

    @FunctionalInterface
    private interface LogicExecutor {
        Map<String, Object> execute() throws Exception;
    }
}