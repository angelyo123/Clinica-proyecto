package com.AutomatizacionService.service.conversacionIA;

import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.client.MedicoClient;
import com.AutomatizacionService.model.CitaDecisionDTO;
import com.AutomatizacionService.service.DeepSeekService;
import com.AutomatizacionService.service.LogicaPaciente.AnalisisSintomasLogic;
import com.AutomatizacionService.service.SugerenciaCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConversacionPacienteService {

    @Autowired private AnalisisSintomasLogic analisisSintomasLogic;
    @Autowired private SugerenciaCacheService cache;
    @Autowired private DeepSeekService deepSeekService;
    @Autowired private MedicoClient medicoClient;
    @Autowired private HorarioClient horarioClient;

    private final ObjectMapper mapper = new ObjectMapper();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public Map<String, Object> procesarConversacion(Map<String, Object> solicitud) {
        try {
            Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());
            String mensaje = solicitud.get("mensaje").toString();

            // 1) Contexto previo
            String contexto = cache.obtenerContextoConversacion(pacienteId);


            // 2) Prompt IA (libre y contextual)
            String prompt = """
Eres un asistente médico virtual empático y experto en comprensión semántica.
Tu tarea es entender el mensaje del paciente, incluso si contiene errores ortográficos o símbolos extraños.

A continuación tienes la lista REAL de médicos registrados en el sistema (extraída directamente de la base de datos).
Cada médico incluye su nombre, especialidad y ID:

%s

Tu rol es analizar el mensaje del paciente y decidir:
- Si menciona una especialidad, corrígela o identifica cuál es la más parecida en la lista.
- Si menciona un síntoma (por ejemplo, "me duele el pecho"), infiere qué especialidad corresponde según los médicos disponibles.
- Si pide una cita o información, responde de manera natural pero clara.
- Siempre que sea posible, asocia la acción a un médico existente (usa su ID).
- Si el paciente menciona un nombre de médico que ya apareció antes, asume que se refiere a ese mismo médico.

Tu respuesta DEBE ser JSON con esta estructura EXACTA:
{
  "respuesta": "mensaje natural para el paciente",
  "accion": "crear_cita | confirmar_cita | cancelar_cita | consultar_horarios | recomendacion | otra",
  "especialidad": "nombre corregido o inferido",
  "parametros": { "fecha": "", "hora": "", "medicoId": "" }
}

Paciente dice: "%s"
""".formatted(obtenerContextoMedicos(), mensaje);



            String respuestaIA = deepSeekService.generarTexto(prompt);
            JsonNode root = mapper.readTree(respuestaIA);
            String contenido = root.path("choices").get(0).path("message").path("content").asText();

            Map<String, Object> decision = mapper.readValue(contenido, Map.class);
            String accion = decision.getOrDefault("accion", "").toString();

            // 3) Actualizar contexto conversacional
            cache.agregarContextoConversacion(pacienteId, "Paciente: " + mensaje);
            cache.agregarContextoConversacion(pacienteId, "IA: " + decision.get("respuesta"));

            // 4) Especialidad desde decisión o contexto
            String especialidad = String.valueOf(decision.getOrDefault("especialidad", "")).trim();
            if (especialidad.isBlank()) {
                Object ctxEsp = cache.obtenerDatoContexto(pacienteId, "especialidad");
                if (ctxEsp != null) {
                    especialidad = String.valueOf(ctxEsp);
                    decision.put("especialidad", especialidad);
                }
            } else {
                cache.guardarDatoContexto(pacienteId, "especialidad", especialidad);
            }

            // 5) Si no hay acción clara
            if (accion.isBlank()) {
                return Map.of("mensaje", decision.getOrDefault("respuesta",
                        "Disculpa, no entendí bien tu mensaje. ¿Podrías repetirlo?"));
            }

            String espIA = String.valueOf(decision.getOrDefault("especialidad", "")).trim();
            if (espIA.isBlank()) {
                Object ctxEsp = cache.obtenerDatoContexto(pacienteId, "especialidad");
                if (ctxEsp != null) {
                    decision.put("especialidad", ctxEsp.toString());
                    System.out.println("🔄 Reforzando especialidad desde contexto: " + ctxEsp);
                }
            }

            Object medIA = decision.get("medicoId");
            if (medIA == null ||
                    (medIA instanceof Number n && n.longValue() <= 0) ||
                    (medIA instanceof String s && (s.isBlank() || s.equals("1")))) {

                Object ctxMed = cache.obtenerDatoContexto(pacienteId, "medicoId");
                if (ctxMed instanceof Number num) {
                    decision.put("medicoId", num.longValue());
                    System.out.println("🔄 Reforzando medicoId desde contexto: " + num.longValue());
                }
            }

            // --- Reforzar medicoId desde contexto de forma definitiva ---
            Object medFinal = decision.get("medicoId");
            if (medFinal == null || "1".equals(String.valueOf(medFinal))) {
                Object ctxMed = cache.obtenerDatoContexto(pacienteId, "medicoId");
                if (ctxMed instanceof Number num) {
                    decision.put("medicoId", num.longValue());
                    System.out.println("🔄 Reforzando medicoId DEFINITIVO desde contexto: " + num.longValue());
                }
            }
            // 6) Flujo por acción
            return switch (accion) {
                case "crear_cita", "confirmar_cita", "cancelar_cita" -> {
                    // ⚙️ Reforzar primero datos críticos ANTES de combinar
                    Object medicoIdRef = decision.get("medicoId");
                    if (medicoIdRef == null || "1".equals(String.valueOf(medicoIdRef))) {
                        Object ctxMed = cache.obtenerDatoContexto(pacienteId, "medicoId");
                        if (ctxMed instanceof Number num) {
                            decision.put("medicoId", num.longValue());
                            System.out.println("🧩 Reasignado medicoId desde contexto (antes de merge): " + num.longValue());
                        }
                    }

                    String espIA2 = String.valueOf(decision.getOrDefault("especialidad", "")).trim();
                    if (espIA2.isBlank()) {
                        Object ctxEsp = cache.obtenerDatoContexto(pacienteId, "especialidad");
                        if (ctxEsp != null) {
                            decision.put("especialidad", ctxEsp.toString());
                            System.out.println("🧩 Reasignada especialidad desde contexto (antes de merge): " + ctxEsp);
                        }
                    }

                    // 🔄 Combinar datos con la solicitud original
                    solicitud.putAll(decision);

                    // Extraer datos principales
                    Long medicoFinal = null;
                    Object medicoRef = decision.get("medicoId");
                    if (medicoRef instanceof Number num) medicoFinal = num.longValue();

                    // ⬇️ Verificar horarios antes de pasar al siguiente paso
                    List<Map<String, Object>> horarios = horarioClient.listarPorMedico(medicoFinal);
                    System.out.println("📅 Verificando disponibilidad del médico " + medicoFinal + ": "
                            + (horarios != null ? horarios.size() : 0));

                    if (horarios == null || horarios.isEmpty()) {
                        yield Map.of(
                                "mensaje", "😕 Lo siento, no hay horarios disponibles con la especialidad de " + decision.get("especialidad") + ".",
                                "propuesta", false,
                                "decisionIA", decision
                        );
                    }

                    // 🧠 Crear DTO con datos consolidados
                    CitaDecisionDTO dto = new CitaDecisionDTO(
                            pacienteId,
                            medicoFinal,
                            String.valueOf(decision.get("especialidad")),
                            String.valueOf(decision.getOrDefault("fecha", LocalDate.now().plusDays(1).toString())),
                            String.valueOf(decision.getOrDefault("hora", "09:00")),
                            "Cita gestionada automáticamente por IA"
                    );

                    System.out.println("🧠 Propagando datos corregidos a AnalisisSintomasLogic → " +
                            "medicoId=" + medicoFinal + ", especialidad=" + dto.getEspecialidad());

                    // ✅ Llamada con el tipo correcto
                    yield analisisSintomasLogic.procesarMensajePaciente(dto);
                }




                case "consultar_horarios" -> procesarConsultaHorarios(pacienteId, decision, mensaje);
                default -> Map.of("mensaje", decision.get("respuesta"));
            };

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error en conversación", "detalle", e.getMessage());
        }
    }

    private String obtenerContextoMedicos() {
        try {
            List<Map<String, Object>> medicos = medicoClient.listarMedicos();
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> m : medicos) {
                sb.append(String.format("- %s → %s (ID: %s)%n",
                        m.get("nombre"), m.get("especialidad"), m.get("id")));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Sin datos médicos disponibles";
        }
    }


    // ===================== LÓGICA DE HORARIOS =====================

    private Map<String, Object> procesarConsultaHorarios(Long pacienteId, Map<String, Object> decision, String mensaje) {
        // 1️⃣ Resolver o memorizar especialidad
        String especialidadRaw = String.valueOf(decision.getOrDefault("especialidad", "")).trim();
        if (!especialidadRaw.isBlank()) {
            cache.guardarDatoContexto(pacienteId, "especialidad", especialidadRaw);
        } else {
            Object ctxEsp = cache.obtenerDatoContexto(pacienteId, "especialidad");
            if (ctxEsp != null) especialidadRaw = String.valueOf(ctxEsp);
        }

        Long medicoIdSugerido = extraerMedicoId(decision);
        if (medicoIdSugerido == null) {
            Object ctxMed = cache.obtenerDatoContexto(pacienteId, "medicoId");
            if (ctxMed instanceof Number) medicoIdSugerido = ((Number) ctxMed).longValue();
        }

        // 🔍 1.5 Detectar si el paciente mencionó un nombre o ID de un médico previamente listado
        List<Map<String, Object>> opciones = (List<Map<String, Object>>) cache.obtenerDatoContexto(pacienteId, "opciones_medicos");
        if (opciones != null && !opciones.isEmpty()) {
            String msgLower = mensaje.toLowerCase();

            for (Map<String, Object> m : opciones) {
                String nombre = String.valueOf(m.get("nombre")).toLowerCase();
                String apellido = String.valueOf(m.getOrDefault("apellido", "")).toLowerCase();
                String idStr = String.valueOf(m.get("id"));

                if (msgLower.contains(nombre) && (apellido.isBlank() || msgLower.contains(apellido)) || msgLower.contains(idStr)) {
                    // ✅ Detectado médico por nombre o ID
                    Long medicoDetectado = Long.parseLong(idStr);
                    cache.guardarDatoContexto(pacienteId, "medicoId", medicoDetectado);
                    System.out.println("✅ Médico identificado por mensaje: " + nombre + " " + apellido + " (ID " + medicoDetectado + ")");
                    medicoIdSugerido = medicoDetectado;
                    break;
                }
            }
        }

        // 2️⃣ Tomar medicoId sugerido por IA o del contexto


        // 3️⃣ Normalizar especialidad (solo para robustez de búsqueda)
        String espNormalizada = normalizarEspecialidad(especialidadRaw);

        System.out.println("🔎 Especialidad recibida (raw): '" + especialidadRaw + "'");
        System.out.println("🔎 Especialidad normalizada:   '" + espNormalizada + "'");
        System.out.println("🔎 medicoId sugerido por IA/contexto: " + medicoIdSugerido);

        Map<String, Object> medicoSeleccionado = null;

        // 4️⃣ Si IA ya trae médico, usarlo directo
        if (medicoIdSugerido != null) {
            medicoSeleccionado = Map.of(
                    "id", medicoIdSugerido,
                    "nombre", cache.obtenerDatoContexto(pacienteId, "nombreMedico") != null
                            ? cache.obtenerDatoContexto(pacienteId, "nombreMedico").toString()
                            : "el médico seleccionado"
            );
            // placeholder
        } else {
            // 5️⃣ Buscar médicos por especialidad
            medicoSeleccionado = resolverMedicoPorEspecialidadFlex(espNormalizada);
            if (medicoSeleccionado == null) {
                String msgEsp = especialidadRaw.isBlank() ? "(sin especialidad indicada)" : especialidadRaw;
                return Map.of("mensaje", "No encontré médicos disponibles para la especialidad: " + msgEsp + ".");
            }

            Object idObj = medicoSeleccionado.get("id");

            // ⚙️ Verificar si se devolvió una lista de médicos
            if (medicoSeleccionado.containsKey("medicos")) {
                List<Map<String, Object>> lista = (List<Map<String, Object>>) medicoSeleccionado.get("medicos");

                // Guardar lista en cache temporal
                cache.guardarDatoContexto(pacienteId, "opciones_medicos", lista);

                // Devolver mensaje natural con las opciones
                return Map.of(
                        "mensaje", medicoSeleccionado.get("mensaje"),
                        "especialidad", medicoSeleccionado.get("especialidad"),
                        "propuesta", true
                );
            }



            if (idObj == null) idObj = medicoSeleccionado.get("medicoId");
            if (idObj == null) idObj = medicoSeleccionado.get("idMedico");

            if (idObj == null) {
                System.out.println("⚠️ No se encontró 'id' en el médico seleccionado: " + medicoSeleccionado);
                return Map.of("mensaje", "No se pudo identificar correctamente al médico de esa especialidad. Intenta nuevamente.");
            }

            medicoIdSugerido = Long.parseLong(idObj.toString());
            cache.guardarDatoContexto(pacienteId, "medicoId", medicoIdSugerido);
        }

        // 6️⃣ Consultar horarios del médico
        List<Map<String, Object>> horarios = horarioClient.listarPorMedico(medicoIdSugerido);
        System.out.println("📅 Horarios obtenidos desde horario-service (medicoId=" + medicoIdSugerido + "): "
                + (horarios != null ? horarios.size() : 0));

        if (horarios == null) {
            return Map.of("mensaje", "⚠️ No se pudieron obtener horarios del sistema en este momento.");
        }

        if (horarios.isEmpty()) {
            String nombre = String.valueOf(medicoSeleccionado.getOrDefault("nombre", "el médico seleccionado"));
            return Map.of("mensaje", "El Dr./Dra. " + nombre + " no tiene horarios disponibles por ahora.");
        }


        // 7️⃣ Unificar todos los horarios disponibles
        List<String> disponibles = new ArrayList<>();
        for (Map<String, Object> h : horarios) {
            try {
                LocalTime inicio = LocalTime.parse(h.get("horaInicio").toString());
                LocalTime fin = LocalTime.parse(h.get("horaFin").toString());
                int pph = (int) h.getOrDefault("pacientesPorHora", 1);
                disponibles.addAll(generarSlots(inicio, fin, pph));
            } catch (Exception ex) {
                System.out.println("⚠️ Error parseando horario: " + h + " -> " + ex.getMessage());
            }
        }

        // 8️⃣ Detectar si el paciente pidió una hora concreta
        LocalTime horaConsultada = detectarHoraEnTexto(mensaje);
        String nombreMedico = String.valueOf(medicoSeleccionado.getOrDefault("nombre", "el médico seleccionado"));

        if (horaConsultada != null) {
            Optional<String> match = disponibles.stream()
                    .filter(h -> LocalTime.parse(h, timeFormatter).equals(horaConsultada))
                    .findFirst();

            if (match.isPresent()) {
                String fecha = LocalDate.now().plusDays(1).toString();
                String hora = horaConsultada.format(timeFormatter);

                CitaDecisionDTO citaDecision = new CitaDecisionDTO(
                        pacienteId,
                        medicoIdSugerido,
                        especialidadRaw,
                        fecha,
                        hora,
                        "Creación automática de cita"
                );

                System.out.println("🤖 [IA] Creando cita con el Dr./Dra. " + nombreMedico +
                        " (" + especialidadRaw + ") a las " + hora + " el " + fecha);

                return analisisSintomasLogic.procesarMensajePaciente(citaDecision);
            } else {
                // Si no hay match exacto, ofrecer hora más cercana
                LocalTime sugerida = obtenerMasCercana(horaConsultada, disponibles);
                return Map.of(
                        "mensaje", "⏰ No hay disponibilidad exacta a las " + horaConsultada.format(timeFormatter)
                                + ", pero puedo ofrecerte a las " + sugerida.format(timeFormatter) + ".",
                        "especialidad", especialidadRaw,
                        "hora", sugerida.format(timeFormatter),
                        "medicoId", medicoIdSugerido
                );
            }
        }

        // 9️⃣ Si no se pidió hora → tomar el primer horario disponible y crear cita automáticamente
        if (!disponibles.isEmpty()) {
            String horariosTexto = String.join(", ", disponibles);

            String mensajeIA = "El Dr./Dra. " + nombreMedico +
                    " tiene horarios disponibles a: " + horariosTexto +
                    ". ¿En cuál de esos horarios deseas tu cita?";

            // Guardamos lista de horarios en cache para la siguiente interacción
            cache.guardarDatoContexto(pacienteId, "horarios_disponibles", disponibles);
            cache.guardarDatoContexto(pacienteId, "medicoId", medicoIdSugerido);
            cache.guardarDatoContexto(pacienteId, "especialidad", especialidadRaw);

            return Map.of(
                    "mensaje", mensajeIA,
                    "especialidad", especialidadRaw,
                    "medicoId", medicoIdSugerido,
                    "propuesta", true
            );
        }

        // 🔚 Si por alguna razón no hay horarios (fallback)
        return Map.of(
                "mensaje", "No se pudo determinar un horario válido para la especialidad " + especialidadRaw + ".",
                "especialidad", especialidadRaw,
                "medicoId", medicoIdSugerido
        );
    }


    // --------- Resolución flexible de médico por especialidad + logging ----------
    private Map<String, Object> resolverMedicoPorEspecialidadFlex(String especialidadNormalizada) {
        List<String> candidatos = variantesEspecialidad(especialidadNormalizada);

        for (String cand : candidatos) {
            List<Map<String, Object>> medicos = medicoClient.listarPorEspecialidad(cand);
            System.out.println("🩺 Buscar por especialidad='" + cand + "': "
                    + (medicos != null ? medicos.size() : 0) + " resultado(s)");

            if (medicos != null && !medicos.isEmpty()) {
                // Log detallado
                for (Map<String, Object> m : medicos) {
                    System.out.println("   • médico: id=" + m.get("id")
                            + ", nombre=" + m.get("nombre")
                            + ", especialidad_raw=" + m.get("especialidad"));
                }

                // Si hay varios médicos, devuelve todos con un mensaje natural
                if (medicos.size() > 1) {
                    StringBuilder sb = new StringBuilder("Encontré varios médicos en esa especialidad:\n");
                    for (Map<String, Object> m : medicos) {
                        sb.append("• ").append(m.get("nombre")).append(" ").append(m.getOrDefault("apellido", ""))
                                .append(" (ID ").append(m.get("id")).append(")\n");
                    }
                    sb.append("¿Con cuál deseas consultar los horarios?");
                    return Map.of(
                            "medicos", medicos,
                            "mensaje", sb.toString(),
                            "especialidad", especialidadNormalizada
                    );
                }

                // Si solo hay uno, devolver directamente el médico
                return medicos.get(0);
            }
        }
        return null;
    }

    private List<String> variantesEspecialidad(String base) {
        // Genera variantes "robustas" para atacar problemas de tildes/encodings/uppercase
        Set<String> set = new LinkedHashSet<>();
        String b = base == null ? "" : base.trim();

        if (!b.isEmpty()) {
            set.add(b);                                 // cardiologia
            set.add(b.toLowerCase());                  // cardiologia
            set.add(b.toUpperCase());                  // CARDIOLOGIA

            // Reintroducir tildes en casos frecuentes
            set.add(b.replace("cardiologia", "cardiología"));
            set.add(b.replace("dermatologia", "dermatología"));
            set.add(b.replace("neurologia", "neurología"));
            set.add(b.replace("pediatria", "pediatría"));
            set.add(b.replace("traumatologia", "traumatología"));

            // Variantes sin/ con guiones o espacios extra
            set.add(b.replace("-", " "));
            set.add(b.replace(" ", ""));

            // Variante con posible mojibake: '¡' por 'i'
            set.add(b.replace("¡", "i"));
        }

        // Si llegó vacío, aún intentamos algunos comunes para no devolver 0 de frente
        if (set.isEmpty()) {
            set.add("cardiologia");
            set.add("cardiología");
            set.add("dermatologia");
            set.add("dermatología");
        }

        return new ArrayList<>(set);
    }

    private String normalizarEspecialidad(String raw) {
        if (raw == null) return "";
        String s = raw.trim()
                .toLowerCase()
                .replace('¡', 'i'); // mojibake frecuente

        // Quitar tildes y diacríticos
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", ""); // remove marks

        // Dejar letras y espacios
        s = s.replaceAll("[^a-z\\s]", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private Long extraerMedicoId(Map<String, Object> decision) {
        // médico sugerido en top-level
        Object top = decision.get("medicoId");
        if (top instanceof Number) return ((Number) top).longValue();
        if (top instanceof String && !((String) top).isBlank()) {
            try { return Long.parseLong((String) top); } catch (NumberFormatException ignored) {}
        }

        // o en "parametros"
        Object params = decision.get("parametros");
        if (params instanceof Map<?, ?> pm) {
            Object mid = pm.get("medicoId");
            if (mid instanceof Number) return ((Number) mid).longValue();
            if (mid instanceof String && !((String) mid).isBlank()) {
                try { return Long.parseLong((String) mid); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    // ===================== UTILIDADES DE HORAS =====================

    private List<String> generarSlots(LocalTime inicio, LocalTime fin, int pacientesPorHora) {
        List<String> slots = new ArrayList<>();
        int minutosPorTurno = Math.max(1, 60 / Math.max(1, pacientesPorHora));
        LocalTime actual = inicio;
        while (!actual.plusMinutes(minutosPorTurno).isAfter(fin)) {
            slots.add(actual.format(timeFormatter));
            actual = actual.plusMinutes(minutosPorTurno);
        }
        return slots;
    }

    private LocalTime detectarHoraEnTexto(String texto) {
        if (texto == null) return null;
        texto = texto.toLowerCase(Locale.ROOT);
        Pattern p = Pattern.compile("(\\d{1,2})(?:[:\\.](\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.|mañana|tarde)?");
        Matcher m = p.matcher(texto);
        if (m.find()) {
            int hora = Integer.parseInt(m.group(1));
            int minutos = (m.group(2) != null) ? Integer.parseInt(m.group(2)) : 0;
            String periodo = m.group(3);

            if (periodo != null && (periodo.contains("p") || periodo.contains("tarde"))) {
                if (hora < 12) hora += 12;
            } else if (periodo != null && (periodo.contains("a") || periodo.contains("mañana"))) {
                if (hora == 12) hora = 0;
            }
            if (hora >= 0 && hora < 24) return LocalTime.of(hora, minutos);
        }
        return null;
    }

    private LocalTime obtenerMasCercana(LocalTime objetivo, List<String> disponibles) {
        return disponibles.stream()
                .map(h -> LocalTime.parse(h, timeFormatter))
                .min(Comparator.comparingLong(h -> Math.abs(h.toSecondOfDay() - objetivo.toSecondOfDay())))
                .orElse(disponibles.isEmpty()
                        ? LocalTime.now()
                        : LocalTime.parse(disponibles.get(0), timeFormatter));
    }
}
