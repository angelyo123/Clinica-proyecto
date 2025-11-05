package com.AutomatizacionService.service.conversacionIA;

import com.AutomatizacionService.service.conversacionIA.context.ConversacionContextService;
import com.AutomatizacionService.service.orquestador.DeepSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ActualizacionMedicosService {

    @Autowired
    private com.AutomatizacionService.service.LogicaMedico.ListarMedicosService listarMedicosService;
    @Autowired private ConversacionContextService contextService;
    @Autowired private DeepSeekService deepSeekService;
    @Autowired private ConversacionPromptBuilder promptBuilder;

    private final ObjectMapper mapper = new ObjectMapper();

    public void verificarYActualizar(Long pacienteId) {
        try {
            Map<String, Object> medicosResponse = listarMedicosService.listarMedicos(pacienteId, Map.of());
            List<Map<String, Object>> listaActual = (List<Map<String, Object>>) medicosResponse.get("data");

            if (listaActual != null && !listaActual.isEmpty()) {
                contextService.guardar(pacienteId, "Sistema (médicos actualizados)",
                        "Lista médica sincronizada con el microservicio.", listaActual);

                // Razonar brevemente sobre la nueva lista
                String contexto = contextService.construirContextoConversacion(pacienteId);
                String memoria = contextService.construirMemoriaPaciente(pacienteId);

                String promptAuto = promptBuilder.construirPrompt(
                        "Analiza la lista actualizada de médicos y resume su disponibilidad al paciente.",
                        contexto,
                        mapper.writeValueAsString(listaActual),
                        memoria
                );

                String razonamientoIA = deepSeekService.generarTexto(promptAuto);
                String razonado = mapper.readTree(razonamientoIA)
                        .path("choices").get(0).path("message").path("content").asText();

                contextService.guardar(pacienteId, "Sistema (razonamiento inmediato)", razonado, listaActual);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error en actualización de médicos: " + e.getMessage());
        }
    }
}
