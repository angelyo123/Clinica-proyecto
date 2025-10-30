package com.AutomatizacionService.service;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.client.MedicoClient;
import com.AutomatizacionService.client.PacienteClient;
import com.AutomatizacionService.model.CitaRequest;
import com.AutomatizacionService.model.DatosCita;
import com.AutomatizacionService.model.SugerenciaPendiente;
import com.AutomatizacionService.repository.SugerenciaPendienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SugerenciaIAService {

    private final DeepSeekService deepSeekService;
    private final PacienteClient pacienteClient;
    private final MedicoClient medicoClient;
    private final HorarioClient horarioClient;
    private final CitaClient citaClient;

    public SugerenciaIAService(
            DeepSeekService deepSeekService,
            PacienteClient pacienteClient,
            MedicoClient medicoClient,
            HorarioClient horarioClient,
            CitaClient citaClient) {
        this.deepSeekService = deepSeekService;
        this.pacienteClient = pacienteClient;
        this.medicoClient = medicoClient;
        this.horarioClient = horarioClient;
        this.citaClient = citaClient;
    }

    @Autowired
    private SugerenciaCacheService sugerenciaCacheService;

    @Autowired
    private SugerenciaPendienteRepository sugerenciaPendienteRepository;

    public Map<String, Object> procesarMensajeNatural(Map<String, Object> solicitud) {
        try {
            String mensajePaciente = solicitud.get("mensaje").toString();
            Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());
            Map<String, Object> paciente = pacienteClient.obtenerPacientePublico(pacienteId);

            // 🩺 1️⃣ Obtener médicos y horarios reales
            List<Map<String, Object>> medicos = medicoClient.listarMedicos();
            List<Map<String, Object>> contextoMedicos = new ArrayList<>();

            for (Map<String, Object> medico : medicos) {
                Long idMedico = Long.valueOf(medico.get("id").toString());
                List<Map<String, Object>> horarios = horarioClient.listarPorMedico(idMedico);
                contextoMedicos.add(Map.of(
                        "id", idMedico,
                        "nombre", medico.get("nombre"),
                        "especialidad", medico.get("especialidad"),
                        "horarios", horarios
                ));
            }

            // 🧠 2️⃣ Crear contexto real para la IA
            String jsonDeDisponibilidad = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(contextoMedicos);

            String hoy = java.time.LocalDate.now().toString();
            String prompt = """
Eres un asistente médico virtual inteligente que trabaja para una clínica real.
Hoy es %s.

Tu tarea es analizar el mensaje del paciente, comprender su problema o síntoma, 
y asignarle una cita con el médico adecuado según las especialidades disponibles.

Estos son los médicos y sus horarios actuales (usa solo los que aparecen aquí):
%s

REGLAS:
1. Analiza el mensaje del paciente: si menciona síntomas o dolencias, identifica la especialidad médica adecuada.
   Ejemplos:
     - "tos", "fiebre", "gripa" → Medicina General
     - "dolor de cabeza", "mareo", "temblores" → Neurología
     - "bulto en la piel", "manchas", "acné", "picazón" → Dermatología
     - "niño", "mi hijo", "bebé", "pediatra" → Pediatría
     - "dolor de pecho", "presión alta", "corazón" → Cardiología
     - "fractura", "dolor en el brazo", "accidente", "caída" → Traumatología
2. Usa exclusivamente los médicos y horarios listados arriba (no inventes médicos).
3. Siempre devuelve el campo "medicoId" con el ID exacto del médico.
4. Si el médico ideal no tiene horario disponible, sugiere el horario más cercano o un médico alternativo de la misma especialidad.
5. Si el paciente pide “mañana”, “el sábado”, “la próxima semana”, calcula la fecha correcta.
6. Devuelve SOLO un JSON válido (sin texto adicional) con este formato EXACTO:

{
  "accion": "crear_cita" | "proponer_nuevo_horario",
  "medicoId": número,
  "especialidad": "texto",
  "fecha": "YYYY-MM-DD",
  "hora": "HH:mm",
  "mensaje": "texto para el paciente"
}

Mensaje del paciente: "%s"
""".formatted(hoy, jsonDeDisponibilidad, mensajePaciente);

            String respuestaIA;
            try {
                respuestaIA = deepSeekService.generarTexto(prompt);
                System.out.println("✅ DeepSeek respondió correctamente");
            } catch (Exception ex) {
                System.err.println("⚠️ Error al conectar con DeepSeek: " + ex.getMessage());
                return Map.of("error", "No se pudo conectar con el servicio de IA.");
            }

            System.out.println("🧩 Respuesta IA: " + respuestaIA);

            // 🧠 4️⃣ Extraer y limpiar el JSON de la respuesta DeepSeek
            String contentJson = null;
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> raw = mapper.readValue(respuestaIA, Map.class);

                // Extrae el contenido del primer choice
                var choices = (List<Map<String, Object>>) raw.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    contentJson = message.get("content").toString();
                }

                // Limpieza: eliminar los ```json y ``` que DeepSeek suele incluir
                if (contentJson != null) {
                    contentJson = contentJson
                            .replaceAll("(?s)```json", "")
                            .replaceAll("```", "")
                            .trim();
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error al extraer JSON interno: " + e.getMessage());
            }

// 🚀 Si no se logró extraer, lanzar error
            if (contentJson == null || !contentJson.trim().startsWith("{")) {
                throw new IllegalArgumentException("La IA no devolvió un JSON válido en el campo 'content'.");
            }

// 🧩 5️⃣ Ahora sí parsear el JSON limpio del contenido
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> decision = mapper.readValue(contentJson, Map.class);
            System.out.println("🧩 Decisión IA parseada (limpia): " + decision);


            // 🧠 6️⃣ Extraer campos del JSON
            String accion = decision.getOrDefault("accion", "crear_cita").toString();
            String especialidad = decision.getOrDefault("especialidad", "Medicina General").toString();
            String fecha = decision.getOrDefault("fecha", java.time.LocalDate.now().plusDays(1).toString()).toString();
            String hora = decision.getOrDefault("hora", "09:00").toString();
            Long medicoId = Long.valueOf(decision.getOrDefault("medicoId", "0").toString());
            String fechaHora = fecha + "T" + hora;

            // 🧩 7️⃣ Si IA no devuelve médicoId válido, buscar por especialidad
            if (medicoId <= 0) {
                String especialidadNormalizada = java.text.Normalizer.normalize(especialidad, java.text.Normalizer.Form.NFD)
                        .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                        .toLowerCase();

                List<Map<String, Object>> medicosCoincidentes = medicos.stream()
                        .filter(m -> {
                            String espMedico = java.text.Normalizer.normalize(
                                            m.get("especialidad").toString(),
                                            java.text.Normalizer.Form.NFD
                                    ).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                                    .toLowerCase();
                            return espMedico.equals(especialidadNormalizada);
                        })
                        .toList();

                if (!medicosCoincidentes.isEmpty()) {
                    medicoId = Long.valueOf(medicosCoincidentes.get(0).get("id").toString());
                    System.out.println("🧩 Asignado médico automáticamente por especialidad: " + especialidad + " -> ID " + medicoId);
                } else {
                    throw new IllegalArgumentException("La IA no devolvió un médico válido ni se encontró uno por especialidad.");
                }
            }

            System.out.println("🤖 IA decidió médico ID: " + medicoId + " (" + especialidad + ")");

            // 🏥 8️⃣ Acciones según decisión
            if ("crear_cita".equalsIgnoreCase(accion)) {
                CitaRequest cita = new CitaRequest(pacienteId, medicoId, fechaHora);
                citaClient.crearCita(cita);

                // Guarda registro histórico en BD
                sugerenciaPendienteRepository.save(SugerenciaPendiente.builder()
                        .pacienteId(pacienteId)
                        .medicoId(medicoId)
                        .fecha(fecha)
                        .hora(hora)
                        .mensaje("Cita creada directamente por IA")
                        .confirmada(true)
                        .build());

                return Map.of(
                        "mensaje", decision.get("mensaje"),
                        "accion", accion
                );
            }

            else if ("proponer_nuevo_horario".equalsIgnoreCase(accion)) {
                // 🕐 Guardar la sugerencia temporalmente en Redis
                Map<String, Object> sugerencia = Map.of(
                        "medicoId", medicoId,
                        "fecha", fecha,
                        "hora", hora
                );
                sugerenciaCacheService.guardarSugerencia(pacienteId, sugerencia);

                // Registrar en BD como pendiente
                sugerenciaPendienteRepository.save(SugerenciaPendiente.builder()
                        .pacienteId(pacienteId)
                        .medicoId(medicoId)
                        .fecha(fecha)
                        .hora(hora)
                        .mensaje(decision.get("mensaje").toString())
                        .confirmada(false)
                        .build());

                return Map.of(
                        "mensaje", decision.get("mensaje"),
                        "accion", accion,
                        "sugerencia", sugerencia
                );
            }

            // ⚙️ Si la acción no es reconocida
            return Map.of("mensaje", "No se pudo interpretar la acción de la IA.");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

    public Map<String, Object> procesarMensajeMedico(Map<String, Object> solicitud) {
        try {
            String mensaje = solicitud.get("mensaje").toString();
            Long medicoId = Long.valueOf(solicitud.get("medicoId").toString());
            String nombreMedico = solicitud.getOrDefault("nombreMedico", "Médico").toString();

            String hoy = java.time.LocalDate.now().toString();

            String prompt = """
Eres un asistente de gestión médica que ayuda a registrar horarios de trabajo de doctores.
Hoy es %s.

El doctor %s ha dicho:
"%s"

Extrae su disponibilidad en este formato JSON exacto:

{
  "accion": "crear_horario_medico",
  "fecha": "YYYY-MM-DD",
  "horaInicio": "HH:mm",
  "horaFin": "HH:mm",
  "pacientesPorHora": número
}

Solo responde con el JSON, sin texto adicional.
""".formatted(hoy, nombreMedico, mensaje);

            String respuestaIA = deepSeekService.generarTexto(prompt);
            System.out.println("🧩 Respuesta IA (médico): " + respuestaIA);

            String jsonLimpio = respuestaIA.replaceAll("^[^\\{]*", "").replaceAll("[^\\}]*$", "");
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> raw = mapper.readValue(jsonLimpio, Map.class);

// 🔍 Extraer el JSON anidado dentro de "choices[0].message.content"
            String contentJson = null;
            try {
                var choices = (List<Map<String, Object>>) raw.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    contentJson = message.get("content").toString();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

// 🧠 Si la IA devolvió el JSON dentro de content, parsea ese
            Map<String, Object> decision;
            if (contentJson != null && contentJson.trim().startsWith("{")) {
                decision = mapper.readValue(contentJson, Map.class);
            } else {
                decision = raw; // fallback si falla
            }

            System.out.println("🧩 Decisión IA parseada: " + decision);


            String fecha = decision.get("fecha").toString();
            String horaInicio = decision.get("horaInicio").toString();
            String horaFin = decision.get("horaFin").toString();
            int pacientesPorHora = Integer.parseInt(decision.get("pacientesPorHora").toString());

            System.out.println("🧠 Creando horario IA para médico " + medicoId + " en fecha " + fecha);

            // Crear bloques horarios por hora
            java.time.LocalTime inicio = java.time.LocalTime.parse(horaInicio);
            java.time.LocalTime fin = java.time.LocalTime.parse(horaFin);

            java.time.LocalTime current = inicio;
            while (current.isBefore(fin)) {
                Map<String, Object> nuevoHorario = Map.of(
                        "diaSemana", java.time.LocalDate.parse(fecha).getDayOfWeek().toString(),
                        "horaInicio", current.toString(),
                        "horaFin", current.plusHours(1).toString(),
                        "disponible", true,
                        "medicoId", medicoId
                );

                var rest = new org.springframework.web.client.RestTemplate();
                rest.postForEntity("http://localhost:8085/horarios", nuevoHorario, Map.class);
                System.out.println("✅ Horario creado: " + nuevoHorario);

                current = current.plusHours(1);
            }

            return Map.of("mensaje", "Se registró el horario del Dr. " + nombreMedico + " exitosamente.");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }
}
