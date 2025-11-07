package com.AutomatizacionService.service.conversacionMedico;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.client.PacienteClient;
import com.AutomatizacionService.service.conversacionIA.context.ConversacionCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConversacionMedicoService {

    @Autowired private ConversacionCoreService coreService;
    @Autowired private CitaClient citaClient;
    @Autowired private HorarioClient horarioClient;
    @Autowired private PacienteClient pacienteClient;

    public Map<String, Object> procesarConversacion(Map<String, Object> solicitud) {
        try {
            Long medicoId = Long.parseLong(solicitud.get("medicoId").toString());
            String mensaje = solicitud.get("mensaje").toString();

            System.out.println("💬 [Conversación-Médico] " + medicoId + " → " + mensaje);

            // Datos combinados
            // Datos combinados
            Map<String, Object> dataFusionada = new HashMap<>();
            dataFusionada.put("citas", citaClient.listarPorMedico(medicoId));
            dataFusionada.put("horarios", horarioClient.listarPorMedico(medicoId));

// Enviar al flujo IA del médico
            return coreService.ejecutarFlujoConversacionMedico(solicitud, dataFusionada);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "❌ Error procesando conversación del médico", "detalle", e.getMessage());
        }
    }
}