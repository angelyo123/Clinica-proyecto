package com.AutomatizacionService.service.conversacionIA.context;

import com.AutomatizacionService.model.entity.ConversacionEntity;
import com.AutomatizacionService.repository.ConversacionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversacionContextService {

    private final ConversacionRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    public ConversacionContextService(ConversacionRepository repo) {
        this.repo = repo;
    }

    public void guardar(Long pacienteId, String usuario, String ia, Object data) {
        try {
            String jsonData = (data != null)
                    ? mapper.writeValueAsString(data)
                    : null;

            repo.save(new ConversacionEntity(pacienteId, usuario, ia, jsonData));

        } catch (Exception e) {
            throw new RuntimeException("Error guardando conversación", e);
        }
    }

    public String construirContextoConversacion(Long pacienteId) {
        List<ConversacionEntity> historial = repo.findByPacienteIdOrderByFechaAsc(pacienteId);
        return historial.stream()
                .map(c -> "Paciente: " + c.getMensajeUsuario() + "\nIA: " + c.getMensajeIA())
                .collect(Collectors.joining("\n"));
    }

    public String obtenerUltimoData(Long pacienteId) {
        List<ConversacionEntity> historial = repo.findByPacienteIdOrderByFechaAsc(pacienteId);
        if (historial.isEmpty()) return null;
        ConversacionEntity ultima = historial.get(historial.size() - 1);
        return ultima.getDataJson();
    }

    public String construirMemoriaPaciente(Long pacienteId) {
        List<ConversacionEntity> historial = repo.findByPacienteIdOrderByFechaAsc(pacienteId);
        if (historial.isEmpty()) return "Sin historial previo.";

        // Tomamos las últimas 5 interacciones para resumir
        String resumen = historial.stream()
                .skip(Math.max(0, historial.size() - 5))
                .map(c -> "Paciente dijo: " + c.getMensajeUsuario() +
                        " | IA respondió: " + c.getMensajeIA())
                .collect(Collectors.joining("\n"));

        return "Resumen del historial reciente del paciente:\n" + resumen;
    }

    public String construirMemoriaMedico(Long medicoId) {
        List<ConversacionEntity> historial = repo.findByPacienteIdOrderByFechaAsc(medicoId);
        if (historial.isEmpty()) return "Sin historial previo.";

        // Tomamos las últimas 5 interacciones para resumir
        String resumen = historial.stream()
                .skip(Math.max(0, historial.size() - 5))
                .map(c -> "Médico dijo: " + c.getMensajeUsuario() +
                        " | IA respondió: " + c.getMensajeIA())
                .collect(Collectors.joining("\n"));

        return "Resumen del historial reciente del médico:\n" + resumen;
    }




}
