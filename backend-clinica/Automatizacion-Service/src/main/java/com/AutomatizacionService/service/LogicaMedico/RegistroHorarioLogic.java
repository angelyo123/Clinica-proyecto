package com.AutomatizacionService.service.LogicaMedico;

import com.AutomatizacionService.service.orquestador.DeepSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RegistroHorarioLogic {

    @Autowired
    private DeepSeekService deepSeekService;

    public Map<String, Object> procesarMensajeMedico(Map<String, Object> solicitud) {
        try {
            String mensaje = solicitud.get("mensaje").toString();
            Long medicoId = Long.valueOf(solicitud.get("medicoId").toString());
            String nombreMedico = solicitud.getOrDefault("nombreMedico", "Médico").toString();

            String prompt = """
Eres un asistente médico que registra horarios.
Extrae un JSON:
{
  "accion": "crear_horario_medico",
  "fecha": "YYYY-MM-DD",
  "horaInicio": "HH:mm",
  "horaFin": "HH:mm"
}
Texto: "%s"
""".formatted(mensaje);

            String respuesta = deepSeekService.generarTexto(prompt);
            var mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(
                    respuesta.replaceAll("```json", "").replaceAll("```", "").trim(),
                    Map.class);

            String fecha = data.get("fecha").toString();
            String horaInicio = data.get("horaInicio").toString();
            String horaFin = data.get("horaFin").toString();

            var rest = new RestTemplate();
            Map<String, Object> horario = Map.of(
                    "diaSemana", java.time.LocalDate.parse(fecha).getDayOfWeek().toString(),
                    "horaInicio", horaInicio,
                    "horaFin", horaFin,
                    "disponible", true,
                    "medicoId", medicoId
            );

            rest.postForEntity("http://localhost:8085/horarios", horario, Map.class);
            return Map.of("mensaje", "🕒 Horario creado exitosamente para " + nombreMedico);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }
}