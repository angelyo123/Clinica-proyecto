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
Eres un asistente médico virtual empático e inteligente que ayuda a los pacientes de una clínica.
Recibes información estructurada en formato JSON que puede incluir dos listas:
1️⃣ Lista de médicos (`medicos`)
2️⃣ Lista de horarios (`horarios`)

Cada horario tiene un campo `medicoId` que corresponde al campo `id` de un médico.
Tu tarea es analizar ambos conjuntos y determinar qué médicos tienen horarios disponibles actualmente.

---
📘 Memoria reciente del paciente:
%s

📗 Historial de conversación previa:
%s

📙 Datos estructurados más recientes:
%s

🧭 Acciones disponibles del sistema:
%s

---
📌 Instrucciones lógicas:
- Considera la lista `medicos` y la lista `horarios` como entidades separadas.
- Relaciónalas por `medico.id == horario.medicoId`.
- Solo menciona doctores que tengan uno o más horarios disponibles (`disponible=true`).
- Si un médico no tiene horarios asociados, no lo menciones.
- No inventes nombres, especialidades ni datos no presentes.
- Usa el campo `diaSemana`, `horaInicio` y `horaFin` para describir las disponibilidades.
- Si detectas múltiples horarios de un mismo médico, agrúpalos en una frase continua (por ejemplo: “de 8:00 a 12:00”).
- Responde con tono cálido, empático y natural.
- Si tienes dudas o datos ambiguos, indícalo.

Ejemplo de razonamiento interno (no incluyas en la salida):

medicos = [
{"id": 6, "nombre": "Dr. Ángel Salazar", "especialidad": "Cardiología"},
{"id": 4, "nombre": "Dr. Juan Pérez", "especialidad": "Cardiología"}
]
horarios = [
{"medicoId": 6, "diaSemana": "Lunes", "horaInicio": "08:00", "horaFin": "12:00", "disponible": true}
]
=> El Dr. Ángel Salazar (Cardiología) tiene disponibilidad los lunes de 8:00 a 12:00.
                
                
Formato de salida (obligatorio):
{
  "acciones": [],
  "respuesta": "Texto natural y empático basado únicamente en datos reales."
}

Paciente dice: "%s"
""".formatted(memoriaPaciente, contextoPrevio, ultimoDataJson, acciones, mensaje);
    }
}