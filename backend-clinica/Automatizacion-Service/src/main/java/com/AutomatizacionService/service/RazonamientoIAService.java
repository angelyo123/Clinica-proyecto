package com.AutomatizacionService.service;

import com.AutomatizacionService.service.conversacionIA.ConversacionPromptBuilder;
import com.AutomatizacionService.service.conversacionIA.context.ConversacionContextService;
import com.AutomatizacionService.service.orquestador.DeepSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RazonamientoIAService {

    @Autowired
    private DeepSeekService deepSeekService;
    @Autowired private ConversacionPromptBuilder promptBuilder;
    @Autowired private ConversacionContextService contextService;
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> generarRazonamientoConDatos(Long pacienteId, Object data, String memoria) {
        try {
            String promptRazonar = promptBuilder.construirPrompt(
                    "Analiza la información médica más reciente y responde al paciente naturalmente.",
                    "",
                    mapper.writeValueAsString(data),
                    memoria
            );

            String respuestaRazonada = deepSeekService.generarTexto(promptRazonar);
            String contenidoRazonado = mapper.readTree(respuestaRazonada)
                    .path("choices").get(0)
                    .path("message").path("content").asText();

            contextService.guardar(pacienteId, "Sistema (razonamiento automático)", contenidoRazonado, null);

            return Map.of("mensaje", contenidoRazonado, "data", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("mensaje", "Hubo un problema al procesar la información médica.");
        }
    }





}
