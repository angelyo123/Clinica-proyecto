package com.AutomatizacionService.service.conversacionIA;

import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.client.MedicoClient;
import com.AutomatizacionService.service.DeepSeekService;
import com.AutomatizacionService.service.LogicaPaciente.AnalisisSintomasLogic;
import com.AutomatizacionService.service.SugerenciaCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
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
Eres un asistente médico virtual empático que conversa con pacientes en lenguaje natural.
Tu rol es interpretar el mensaje y decidir la acción adecuada (crear cita, confirmar, cancelar, consultar, etc.).
No respondas con texto fuera del JSON.

Estructura JSON esperada:
{
  "respuesta": "mensaje natural para el paciente",
  "accion": "crear_cita | confirmar_cita | cancelar_cita | consultar_horarios | recomendacion | otra",
  "especialidad": "si aplica",
  "parametros": { "fecha": "", "hora": "", "medicoId": "" }
}

Contexto previo:
%s

Paciente dice: "%s"
""".formatted(contexto, mensaje);

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

                    // 🔄 Ahora sí combinar datos
                    solicitud.putAll(decision);

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

                    solicitud.put("medicoId", medicoFinal);
                    solicitud.put("especialidad", decision.get("especialidad"));

                    System.out.println("🧠 Propagando datos corregidos a AnalisisSintomasLogic → medicoId=" + medicoFinal);
                    yield analisisSintomasLogic.procesarMensajePaciente(solicitud);
                }


                case "consultar_horarios" -> procesarConsultaHorarios(pacienteId, decision, mensaje);
                default -> Map.of("mensaje", decision.get("respuesta"));
            };

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error en conversación", "detalle", e.getMessage());
        }
    }

    // ===================== LÓGICA DE HORARIOS =====================

    private Map<String, Object> procesarConsultaHorarios(Long pacienteId, Map<String, Object> decision, String mensaje) {
        // 1) Resolver o memorizar especialidad
        String especialidadRaw = String.valueOf(decision.getOrDefault("especialidad", "")).trim();
        if (!especialidadRaw.isBlank()) {
            cache.guardarDatoContexto(pacienteId, "especialidad", especialidadRaw);
        } else {
            Object ctxEsp = cache.obtenerDatoContexto(pacienteId, "especialidad");
            if (ctxEsp != null) especialidadRaw = String.valueOf(ctxEsp);
        }

        // 2) Tomar medicoId sugerido por IA (si lo envía) o por contexto
        Long medicoIdSugerido = extraerMedicoId(decision);
        if (medicoIdSugerido == null) {
            Object ctxMed = cache.obtenerDatoContexto(pacienteId, "medicoId");
            if (ctxMed instanceof Number) medicoIdSugerido = ((Number) ctxMed).longValue();
        }

        // 3) Normalizar especialidad para robustez (quita tildes/símbolos)
        String espNormalizada = normalizarEspecialidad(especialidadRaw);

        System.out.println("🔎 Especialidad recibida (raw): '" + especialidadRaw + "'");
        System.out.println("🔎 Especialidad normalizada:   '" + espNormalizada + "'");
        System.out.println("🔎 medicoId sugerido por IA/contexto: " + medicoIdSugerido);

        Map<String, Object> medicoSeleccionado = null;

        // 4) Si IA trae medicoId, usarlo directo
        if (medicoIdSugerido != null) {
            // (Opcional) si tienes endpoint para obtener médico por ID, úsalo aquí.
            // Si no, continuamos directo a consultar horarios por ID.
            medicoSeleccionado = Map.of("id", medicoIdSugerido, "nombre", ""); // placeholder para mensajes
        } else {
            // 5) Buscar médicos por especialidad con intentos flexibles
            medicoSeleccionado = resolverMedicoPorEspecialidadFlex(espNormalizada);
            if (medicoSeleccionado == null) {
                String msgEsp = especialidadRaw.isBlank() ? "(sin especialidad indicada)" : especialidadRaw;
                return Map.of("mensaje",
                        "No encontré médicos disponibles para la especialidad: " + msgEsp + ".");
            }
            medicoIdSugerido = ((Number) medicoSeleccionado.get("id")).longValue();
            cache.guardarDatoContexto(pacienteId, "medicoId", medicoIdSugerido);
        }

        // 6) Traer horarios del médico resuelto
        List<Map<String, Object>> horarios = horarioClient.listarPorMedico(medicoIdSugerido);
        System.out.println("📅 Horarios obtenidos desde horario-service (medicoId=" + medicoIdSugerido + "): "
                + (horarios != null ? horarios.size() : 0));

        if (horarios == null || horarios.isEmpty()) {
            String nombre = String.valueOf(medicoSeleccionado.getOrDefault("nombre", "el médico seleccionado"));
            return Map.of("mensaje", "El Dr./Dra. " + nombre + " no tiene horarios disponibles por ahora.");
        }

        // 7) Unificar slots disponibles
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

        // 8) Detectar si el paciente pidió una hora concreta
        LocalTime horaConsultada = detectarHoraEnTexto(mensaje);

        String nombreMedico = String.valueOf(medicoSeleccionado.getOrDefault("nombre", "el médico seleccionado"));
        if (horaConsultada != null) {
            Optional<String> match = disponibles.stream()
                    .filter(h -> LocalTime.parse(h, timeFormatter).equals(horaConsultada))
                    .findFirst();
            if (match.isPresent()) {
                return Map.of(
                        "mensaje", "✅ Sí, hay disponibilidad a las " + horaConsultada.format(timeFormatter) +
                                ". ¿Deseas que te agende con el Dr./Dra. " + nombreMedico + "?",
                        "especialidad", especialidadRaw,
                        "hora", horaConsultada.format(timeFormatter),
                        "medicoId", medicoIdSugerido
                );
            } else {
                LocalTime sugerida = obtenerMasCercana(horaConsultada, disponibles);
                return Map.of(
                        "mensaje", "⏰ No hay disponibilidad exacta a las " + horaConsultada.format(timeFormatter) +
                                ", pero puedo ofrecerte a las " + sugerida.format(timeFormatter) + ".",
                        "especialidad", especialidadRaw,
                        "hora", sugerida.format(timeFormatter),
                        "medicoId", medicoIdSugerido
                );
            }
        }

        // 9) Si no se mencionó hora, listar todo
        return Map.of(
                "mensaje", "El Dr./Dra. " + nombreMedico + " tiene horarios disponibles a: " +
                        String.join(", ", disponibles),
                "especialidad", especialidadRaw,
                "medicoId", medicoIdSugerido
        );
    }

    // --------- Resolución flexible de médico por especialidad + logging ----------
    private Map<String, Object> resolverMedicoPorEspecialidadFlex(String especialidadNormalizada) {
        // Intentos de búsqueda con diferentes variantes del texto
        List<String> candidatos = variantesEspecialidad(especialidadNormalizada);

        for (String cand : candidatos) {
            List<Map<String, Object>> medicos = medicoClient.listarPorEspecialidad(cand);
            System.out.println("🩺 Buscar por especialidad='" + cand + "': "
                    + (medicos != null ? medicos.size() : 0) + " resultado(s)");
            if (medicos != null && !medicos.isEmpty()) {
                // Log detallado de lo que trae la BD (id/nombre/especialidad cruda)
                for (Map<String, Object> m : medicos) {
                    System.out.println("   • médico: id=" + m.get("id")
                            + ", nombre=" + m.get("nombre")
                            + ", especialidad_raw=" + m.get("especialidad"));
                }
                // Elegimos el primero (puedes cambiar a otra estrategia)
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
