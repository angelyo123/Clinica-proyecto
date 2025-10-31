package com.AutomatizacionService.service.conversacionIA;

import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.client.MedicoClient;
import com.AutomatizacionService.model.dto.CitaDecisionDTO;
import com.AutomatizacionService.service.LogicaPaciente.AnalisisSintomasLogic;
import com.AutomatizacionService.service.orquestador.SugerenciaCacheService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AccionHandlers {

    private final MedicoClient medicoClient;
    private final HorarioClient horarioClient;
    private final AnalisisSintomasLogic analisisSintomasLogic;
    private final SugerenciaCacheService cache;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public AccionHandlers(MedicoClient medicoClient,
                          HorarioClient horarioClient,
                          AnalisisSintomasLogic analisisSintomasLogic,
                          SugerenciaCacheService cache) {
        this.medicoClient = medicoClient;
        this.horarioClient = horarioClient;
        this.analisisSintomasLogic = analisisSintomasLogic;
        this.cache = cache;
    }

    // === Acciones principales ===

    public Map<String, Object> listarMedicos(Long pacienteId, Map<String, Object> params) {
        System.out.println("\n📋 [Acción] listar_medicos → pacienteId=" + pacienteId);

        List<Map<String, Object>> medicos = medicoClient.listarMedicos();
        System.out.println("👨‍⚕️ Total médicos obtenidos: " + (medicos != null ? medicos.size() : 0));

        StringBuilder sb = new StringBuilder("👨‍⚕️ Médicos disponibles:\n");
        for (Map<String, Object> m : medicos) {
            sb.append("- ").append(m.get("nombre"))
                    .append(" (").append(m.get("especialidad")).append(")\n");
        }
        return Map.of("mensaje", sb.toString());
    }

    public Map<String, Object> consultarHorarios(Long pacienteId, Map<String, Object> decision) {
        System.out.println("\n🩺 [Acción] consultar_horarios → pacienteId=" + pacienteId);
        try {
            System.out.println("🧩 Datos de entrada IA: " + decision);

            // 🧠 1️⃣ Revisión directa: ¿el mensaje o los parámetros mencionan un médico específico?
            Map<String, Object> parametros = (Map<String, Object>) decision.get("parametros");
            String nombreMedicoParam = null;
            if (parametros != null && parametros.containsKey("medico")) {
                nombreMedicoParam = String.valueOf(parametros.get("medico")).toLowerCase();
            }

            String textoPaciente = String.valueOf(decision.getOrDefault("respuesta", "")).toLowerCase();

// 📡 Obtener lista completa de médicos (una sola vez)
            List<Map<String, Object>> todosMedicos = medicoClient.listarMedicos();
            if (todosMedicos == null || todosMedicos.isEmpty()) {
                return Map.of("mensaje", "No hay médicos registrados en el sistema por ahora.");
            }

// 🩺 Buscar coincidencia por nombre de médico
            if (nombreMedicoParam != null || textoPaciente.contains("dr") || textoPaciente.contains("dra")) {
                String nombreBuscar = nombreMedicoParam != null ? nombreMedicoParam : textoPaciente;

                Optional<Map<String, Object>> coincidencia = todosMedicos.stream()
                        .filter(m -> {
                            String nombreMedico = String.valueOf(m.get("nombre")).toLowerCase();
                            // tolerancia a acentos y errores
                            return normalizar(nombreMedico).contains(normalizar(nombreBuscar))
                                    || normalizar(nombreBuscar).contains(normalizar(nombreMedico));
                        })
                        .findFirst();

                if (coincidencia.isPresent()) {
                    Map<String, Object> medico = coincidencia.get();
                    Long medicoId = Long.parseLong(medico.get("id").toString());
                    List<Map<String, Object>> horarios = horarioClient.listarPorMedico(medicoId);

                    if (horarios == null || horarios.isEmpty()) {
                        return Map.of("mensaje", "El Dr./Dra. " + medico.get("nombre")
                                + " no tiene horarios disponibles actualmente.");
                    }

                    List<String> disponibles = generarSlotsDeHorarios(horarios);
                    cache.guardarDatoContexto(pacienteId, "medicoId", medicoId);
                    cache.guardarDatoContexto(pacienteId, "especialidad", medico.get("especialidad"));
                    cache.guardarDatoContexto(pacienteId, "horarios_disponibles", disponibles);

                    String texto = "El Dr./Dra. " + medico.get("nombre") +
                            " tiene horarios disponibles a: " + String.join(", ", disponibles) +
                            ". ¿En cuál deseas agendar tu cita?";

                    System.out.println("✅ Horarios obtenidos directamente por nombre: " + medico.get("nombre"));
                    return Map.of("mensaje", texto, "medicoId", medicoId);
                } else {
                    System.out.println("⚠️ No se encontró médico con el nombre: " + nombreBuscar);
                }
            }


            // 🧠 Verificar si ya hay un médico y horarios guardados en contexto
            Object ctxMedico = cache.obtenerDatoContexto(pacienteId, "medicoId");
            Object ctxHorarios = cache.obtenerDatoContexto(pacienteId, "horarios_disponibles");

            if (ctxMedico != null && ctxHorarios instanceof List<?>) {
                System.out.println("🔁 Contexto previo detectado → ya hay médico y horarios guardados.");

                // Intentar extraer hora desde el mensaje
                String mensajePaciente = String.valueOf(decision.getOrDefault("respuesta", ""));
                String horaDetectada = ConversacionUtils.extraerHoraDesdeTexto(mensajePaciente);

                if (horaDetectada != null) {
                    System.out.println("🕒 Hora detectada: " + horaDetectada + " → creando cita directamente.");
                    cache.guardarDatoContexto(pacienteId, "hora_detectada", horaDetectada);
                    Long medicoId = Long.parseLong(ctxMedico.toString());
                    String especialidad = String.valueOf(cache.obtenerDatoContexto(pacienteId, "especialidad"));
                    String fecha = LocalDate.now().plusDays(1).toString();

                    CitaDecisionDTO dto = new CitaDecisionDTO(
                            pacienteId, medicoId, especialidad, fecha, horaDetectada,
                            "Cita confirmada automáticamente desde contexto IA"
                    );

                    Map<String, Object> resultado = analisisSintomasLogic.procesarMensajePaciente(dto);
                    System.out.println("✅ Cita creada automáticamente desde contexto: " + resultado);
                    return resultado;
                }
            }


            // 🔎 1) Toma la especialidad primero de parametros, luego del toplevel, luego del cache, etc.
            String especialidad = extraerEspecialidad(decision, pacienteId);

            if (especialidad == null || especialidad.isBlank()) {
                System.out.println("⚠️ No se pudo determinar la especialidad.");
                return Map.of("mensaje",
                        "No pude identificar la especialidad. ¿Podrías decirme si es para cardiología, pediatría o dermatología?");
            }

            System.out.println("🔎 Buscando horarios para especialidad: " + especialidad);

            // Normaliza mínimamente (acentos/símbolos)
            especialidad = normalizarNombreEspecialidad(especialidad);

            // 📡 Llama al microservicio de médicos
            System.out.println("📡 Solicitando lista completa de médicos a microservicio...");


            // 🧠 Detectar si el paciente mencionó un nombre de médico en el mensaje
            Optional<Map<String, Object>> medicoPorNombre = todosMedicos.stream()
                    .filter(m -> {
                        String nombre = String.valueOf(m.get("nombre")).toLowerCase();
                        return textoPaciente.contains(nombre.split(" ")[0].toLowerCase()) ||
                                textoPaciente.contains(nombre.replace("dr.", "").trim());
                    })
                    .findFirst();

            if (medicoPorNombre.isPresent()) {
                Map<String, Object> medico = medicoPorNombre.get();
                Long medicoId = Long.parseLong(medico.get("id").toString());
                List<Map<String, Object>> horarios = horarioClient.listarPorMedico(medicoId);

                if (horarios == null || horarios.isEmpty()) {
                    return Map.of("mensaje", "El Dr./Dra. " + medico.get("nombre") +
                            " no tiene horarios disponibles actualmente.");
                }

                List<String> disponibles = generarSlotsDeHorarios(horarios);
                cache.guardarDatoContexto(pacienteId, "medicoId", medicoId);
                cache.guardarDatoContexto(pacienteId, "especialidad", medico.get("especialidad"));
                cache.guardarDatoContexto(pacienteId, "horarios_disponibles", disponibles);

                String texto = "El Dr./Dra. " + medico.get("nombre") +
                        " tiene horarios disponibles a: " + String.join(", ", disponibles) +
                        ". ¿En cuál deseas agendar tu cita?";
                System.out.println("✅ Horarios obtenidos directamente por nombre del médico.");
                return Map.of("mensaje", texto);
            }


            if (todosMedicos == null || todosMedicos.isEmpty()) {
                return Map.of("mensaje", "No hay médicos registrados en el sistema por ahora.");
            }

// 🔎 Filtrado flexible que ignora acentos y mayúsculas
            String espNormalizada = normalizar(especialidad);
            List<Map<String, Object>> medicos = todosMedicos.stream()
                    .filter(m -> {
                        String espMed = normalizar(String.valueOf(m.get("especialidad")));
                        return espMed.contains(espNormalizada)
                                || espNormalizada.contains(espMed)
                                || espMed.startsWith(espNormalizada.substring(0, Math.min(5, espNormalizada.length())));

                    })
                    .toList();

            System.out.println("👨‍⚕️ Médicos encontrados (tolerante a escritura): " + medicos.size());


            System.out.println("👨‍⚕️ Médicos encontrados para '" + especialidad + "': " +
                    (medicos == null ? "null" : medicos.size()));

            if (medicos == null || medicos.isEmpty()) {
                System.out.println("⚠️ Ningún médico coincide con la especialidad: " + especialidad);
                return Map.of("mensaje", "No hay médicos disponibles para la especialidad " + especialidad + ".");
            }

            // 🔍 Buscar cuáles de esos médicos realmente tienen horarios
            List<Map<String, Object>> medicosConHorarios = new ArrayList<>();

            for (Map<String, Object> m : medicos) {
                Long idMedico = Long.parseLong(m.get("id").toString());
                List<Map<String, Object>> horarios = horarioClient.listarPorMedico(idMedico);

                if (horarios != null && !horarios.isEmpty()) {
                    m.put("horarios", horarios);
                    medicosConHorarios.add(m);
                    System.out.println("✅ Médico con horarios: " + m.get("nombre") +
                            " (" + m.get("especialidad") + ") → " + horarios.size() + " horarios.");
                } else {
                    System.out.println("⚠️ Médico sin horarios: " + m.get("nombre"));
                }
            }

            // 🚨 Si ninguno tiene horarios
            if (medicosConHorarios.isEmpty()) {
                return Map.of("mensaje", "Ninguno de los médicos de " + especialidad +
                        " tiene horarios disponibles por ahora. ¿Deseas que te avise cuando se libere uno?");
            }

            // 👥 Si hay varios con horarios → ofrecer elección
            if (medicosConHorarios.size() > 1) {
                StringBuilder sb = new StringBuilder("He encontrado varios médicos de ")
                        .append(especialidad)
                        .append(" con horarios disponibles:\n\n");

                for (Map<String, Object> m : medicosConHorarios) {
                    Long id = Long.parseLong(m.get("id").toString());
                    List<Map<String, Object>> h = (List<Map<String, Object>>) m.get("horarios");
                    List<String> disponibles = generarSlotsDeHorarios(h);
                    sb.append("- [ID ").append(id).append("] ").append(m.get("nombre"))
                            .append(" → ").append(String.join(", ", disponibles)).append("\n");
                }

                sb.append("\nPor favor, indícame con qué médico deseas agendar tu cita (puedes decir su nombre o ID).");

                // Guardamos la lista para próximos pasos
                cache.guardarDatoContexto(pacienteId, "opciones_medicos", medicosConHorarios);

                return Map.of("mensaje", sb.toString(), "especialidad", especialidad);
            }

            // 👤 Si solo hay uno con horarios → usarlo directamente
            Map<String, Object> medico = medicosConHorarios.get(0);
            Long medicoId = Long.parseLong(medico.get("id").toString());
            cache.guardarDatoContexto(pacienteId, "medicoId", medicoId);
            cache.guardarDatoContexto(pacienteId, "especialidad", especialidad);

            List<Map<String, Object>> horarios = (List<Map<String, Object>>) medico.get("horarios");
            List<String> disponibles = generarSlotsDeHorarios(horarios);
            cache.guardarDatoContexto(pacienteId, "horarios_disponibles", disponibles);

            String texto = "El Dr./Dra. " + medico.get("nombre") +
                    " tiene horarios disponibles a: " + String.join(", ", disponibles) +
                    ". ¿En cuál deseas agendar tu cita?";

            System.out.println("✅ Horarios generados correctamente para " + medico.get("nombre"));
            return Map.of("mensaje", texto, "especialidad", especialidad, "medicoId", medicoId);

        } catch (Exception e) {
            System.out.println("❌ Error interno en consultarHorarios: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Error al consultar horarios", "detalle", e.getMessage());
        }
    }


    /** Lee especialidad en este orden: parametros → toplevel → cache → nombre directo en texto → heurística */
    private String extraerEspecialidad(Map<String, Object> decision, Long pacienteId) {
        // 1) parámetros
        Object paramsObj = decision.get("parametros");
        if (paramsObj instanceof Map<?, ?> params) {
            Object esp = params.get("especialidad");
            if (esp != null && !esp.toString().isBlank()) {
                return esp.toString();
            }
        }

        // 2) nivel superior
        Object top = decision.get("especialidad");
        if (top != null && !top.toString().isBlank()) {
            return top.toString();
        }

        // 3) cache
        Object ctxEsp = cache.obtenerDatoContexto(pacienteId, "especialidad");
        if (ctxEsp != null && !ctxEsp.toString().isBlank()) {
            return ctxEsp.toString();
        }

        // 4) por nombre directo en texto
        String texto = (decision.getOrDefault("respuesta", "") + " " +
                String.valueOf(cache.obtenerContextoConversacion(pacienteId)));
        String porNombre = ConversacionUtils.detectarEspecialidadPorNombre(texto);
        if (porNombre != null) return porNombre;

        // 5) heurística
        return ConversacionUtils.deducirEspecialidad(texto);
    }






    /** Normaliza variantes de nombre de especialidad a su forma canónica con acento */
    private String normalizarNombreEspecialidad(String esp) {
        String t = normalizar(esp);
        if (t.contains("cardiolog")) return "Cardiología";
        if (t.contains("dermatolog") || t.contains("derma")) return "Dermatología";
        if (t.contains("pediatr")) return "Pediatría";
        if (t.contains("neurolog")) return "Neurología";
        if (t.contains("traumatolog") || t.contains("trauma")) return "Traumatología";
        if (t.contains("medicina") && t.contains("general")) return "Medicina General";
        if (t.contains("oftalmolog") || t.contains("oftalmo")) return "Oftalmología";
        if (t.contains("odontolog") || t.contains("dental")) return "Odontología";
        if (t.contains("otorrino") || t.contains("otorrinolaringolog")) return "Otorrinolaringología";
        return esp; // por defecto, respeta lo que vino
    }

    public Map<String, Object> crearCita(Long pacienteId, Map<String, Object> decision) {
        System.out.println("\n📅 [Acción] crear_cita → pacienteId=" + pacienteId);
        try {





            Long medicoId = extraerMedicoId(pacienteId, decision);
            System.out.println("🩺 MédicoId detectado: " + medicoId);
            String mensaje = String.valueOf(decision.getOrDefault("respuesta", ""));
            String horaDetectada = ConversacionUtils.extraerHoraDesdeTexto(mensaje);

            String especialidad = String.valueOf(decision.getOrDefault("especialidad", ""));
            String fecha = String.valueOf(decision.getOrDefault("fecha", LocalDate.now().plusDays(1).toString()));

            String hora = String.valueOf(decision.getOrDefault("hora", ""));
            if (horaDetectada != null && hora.isBlank()) {
                hora = horaDetectada;
            }

            if (hora.isBlank()) {
                Object ctxHora = cache.obtenerDatoContexto(pacienteId, "hora_detectada");
                if (ctxHora != null) hora = ctxHora.toString();
            }
            if (hora == null || hora.isBlank()) hora = "09:00";

            System.out.println("📋 Datos para cita → MedicoID=" + medicoId + ", Fecha=" + fecha + ", Hora=" + hora);

            CitaDecisionDTO dto = new CitaDecisionDTO(
                    pacienteId, medicoId, especialidad, fecha, hora,
                    "Cita gestionada automáticamente por IA"
            );
            Map<String, Object> resultado = analisisSintomasLogic.procesarMensajePaciente(dto);
            System.out.println("✅ Cita creada correctamente: " + resultado);
            return resultado;

        } catch (Exception e) {
            System.out.println("❌ Error interno en crearCita: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Error al crear cita", "detalle", e.getMessage());
        }
    }

    public Map<String, Object> cancelarCita(Long pacienteId, Map<String, Object> params) {
        System.out.println("\n🗓️ [Acción] cancelar_cita → pacienteId=" + pacienteId);
        return Map.of("mensaje", "Tu cita ha sido cancelada exitosamente.");
    }

    // === Helpers ===

    private Long extraerMedicoId(Long pacienteId, Map<String, Object> decision) {
        Object top = decision.get("medicoId");
        if (top instanceof Number) return ((Number) top).longValue();
        if (top instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s); } catch (Exception ignored) {}
        }
        Object ctxMed = cache.obtenerDatoContexto(pacienteId, "medicoId");
        if (ctxMed instanceof Number num) return num.longValue();
        return null;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        // Normaliza y elimina acentos
        String limpio = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .trim();

        // Limpieza extendida: elimina símbolos, signos y espacios dobles
        limpio = limpio
                .replaceAll("[^a-z]", "")       // solo deja letras
                .replaceAll("\\s+", "");        // borra espacios sobrantes

        return limpio;
    }


    private List<String> generarSlotsDeHorarios(List<Map<String, Object>> horarios) {
        List<String> slots = new ArrayList<>();
        for (Map<String, Object> h : horarios) {
            try {
                LocalTime inicio = LocalTime.parse(h.get("horaInicio").toString());
                LocalTime fin = LocalTime.parse(h.get("horaFin").toString());
                int pph = (int) h.getOrDefault("pacientesPorHora", 1);
                int minutosPorTurno = Math.max(1, 60 / Math.max(1, pph));
                LocalTime actual = inicio;
                while (!actual.plusMinutes(minutosPorTurno).isAfter(fin)) {
                    slots.add(actual.format(timeFormatter));
                    actual = actual.plusMinutes(minutosPorTurno);
                }
            } catch (Exception ex) {
                System.out.println("⚠️ Error generando slot de horario: " + ex.getMessage());
            }
        }
        return slots;
    }
}
