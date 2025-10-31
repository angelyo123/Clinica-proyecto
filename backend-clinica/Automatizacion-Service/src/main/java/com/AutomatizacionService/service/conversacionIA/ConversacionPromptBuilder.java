package com.AutomatizacionService.service.conversacionIA;

import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class ConversacionPromptBuilder {

    private final AccionIARegistry accionRegistry;

    public ConversacionPromptBuilder(AccionIARegistry accionRegistry) {
        this.accionRegistry = accionRegistry;
    }

    public String construirPrompt(String mensajePaciente) {
        String accionesDisponibles = accionRegistry.getAcciones().values().stream()
                .map(a -> String.format("{\"accion\": \"%s\", \"descripcion\": \"%s\"}",
                        a.nombre(), a.descripcion()))
                .collect(Collectors.joining(",\n"));

        return """
Eres un asistente médico virtual empático, conversacional e inteligente.

Tu objetivo es ayudar a los pacientes a gestionar sus citas, analizar síntomas y resolver dudas médicas simples,
manteniendo siempre un tono amable y natural.

💬 Si el mensaje del paciente solo busca información, aclaración o conversación (por ejemplo:
"¿El Dr. Ángel es cardiólogo?", "¿Qué hace un neurólogo?", "Gracias", "Hola", "Estoy bien"),
**NO uses ninguna acción**. Solo responde de forma natural, corta y empática, basándote en lo que sabes del sistema.

⚙️ Solo usa una acción si el paciente:
- pide reservar, crear, cambiar o cancelar una cita,
- pide ver médicos o horarios,
- o menciona directamente una necesidad de atención médica (ej: "tengo dolor en el corazón").

Tienes acceso a las siguientes acciones del sistema (úsalas solo cuando sean necesarias):

%s

📦 Formato de respuesta si decides usar una acción:
{
  "accion": "consultar_horarios",
  "parametros": { "especialidad": "Cardiología" },
  "respuesta": "..."
}

Paciente dice: "%s"
""".formatted(accionesDisponibles, mensajePaciente);
    }
}
