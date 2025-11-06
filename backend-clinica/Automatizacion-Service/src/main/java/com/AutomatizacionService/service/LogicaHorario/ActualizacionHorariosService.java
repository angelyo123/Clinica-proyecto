package com.AutomatizacionService.service.LogicaHorario;

import com.AutomatizacionService.service.conversacionIA.ConversacionPromptBuilder;
import com.AutomatizacionService.service.conversacionIA.context.ConversacionContextService;
import com.AutomatizacionService.service.orquestador.DeepSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ActualizacionHorariosService {

    @Autowired private ListarHorariosService listarHorariosService;
    @Autowired private ConversacionContextService contextService;
    @Autowired private DeepSeekService deepSeekService;
    @Autowired private ConversacionPromptBuilder promptBuilder;

    private final ObjectMapper mapper = new ObjectMapper();

    public void verificarYActualizar(Long pacienteId) {
        try {
            Map<String, Object> horariosResponse = listarHorariosService.listarHorarios(pacienteId, Map.of());
            List<Map<String, Object>> listaActual = (List<Map<String, Object>>) horariosResponse.get("data");

            if (listaActual != null && !listaActual.isEmpty()) {
                contextService.guardar(pacienteId, "Sistema (horarios actualizados)",
                        "Lista de horarios sincronizada con el microservicio.", listaActual);

                String contexto = contextService.construirContextoConversacion(pacienteId);
                String memoria = contextService.construirMemoriaPaciente(pacienteId);

                String promptAuto = promptBuilder.construirPrompt(
                        "Analiza la lista actualizada de horarios y resume su disponibilidad al paciente.",
                        contexto,
                        mapper.writeValueAsString(listaActual),
                        memoria
                );

                String razonamientoIA = deepSeekService.generarTexto(promptAuto);
                String razonado = mapper.readTree(razonamientoIA)
                        .path("choices").get(0).path("message").path("content").asText();

                contextService.guardar(pacienteId, "Sistema (razonamiento inmediato horarios)", razonado, listaActual);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error en actualización de horarios: " + e.getMessage());
        }
    }
}