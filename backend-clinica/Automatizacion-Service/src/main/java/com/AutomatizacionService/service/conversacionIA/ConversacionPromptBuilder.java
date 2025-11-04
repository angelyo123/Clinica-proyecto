package com.AutomatizacionService.service.conversacionIA;

import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class ConversacionPromptBuilder {

    private final AccionIARegistry accionRegistry;

    public ConversacionPromptBuilder(AccionIARegistry accionRegistry) {
        this.accionRegistry = accionRegistry;
    }


    public String construirPrompt(String mensaje, String contextoPrevio, String ultimoDataJson, String memoriaPaciente) {
        String acciones = accionRegistry.getAcciones().values().stream()
                .map(a -> String.format("{\"accion\": \"%s\", \"descripcion\": \"%s\"}",
                        a.nombre(), a.descripcion()))
                .collect(Collectors.joining(",\n"));

        return """
Eres un asistente médico virtual empático e inteligente, capaz de recordar conversaciones anteriores con cada paciente.

Información de memoria (resumen de historial del paciente):
%s

Tu objetivo es ayudar al paciente a gestionar información médica de su clínica: médicos, especialidades, horarios y citas.

El historial previo de conversación es:
%s

Los datos estructurados más recientes (por ejemplo, una lista de médicos u horarios) son:
%s

Si el paciente pregunta algo personal como "¿te acuerdas de mí?", usa la memoria para responder con empatía y coherencia.

Las acciones disponibles son:
%s

➡️ Si estos datos existen, úsalos para responder razonando directamente sobre ellos.
Por ejemplo, puedes contar médicos, filtrar por especialidad o nombrar doctores específicos SIN usar una acción.

Solo usa una acción del sistema si el paciente pide explícitamente realizar una operación nueva
(como listar, crear o cancelar citas).

IMPORTANTE:
Responde SIEMPRE en este formato JSON:
{
  "accion": "listar_medicos" | "" | "otra_accion",
  "parametros": {},
  "respuesta": "Texto natural y empático que responda al paciente"
}

Reglas:
- Si ya tienes datos en “Los datos estructurados recientes”, puedes analizarlos directamente.
- Si el paciente solo pregunta cosas sobre esos datos (ej. '¿cuántos médicos hay?'), NO uses una acción.
- No inventes nombres, cantidades ni especialidades.

Paciente dice: "%s"
""".formatted(memoriaPaciente, contextoPrevio, ultimoDataJson, acciones, mensaje);
    }
}
