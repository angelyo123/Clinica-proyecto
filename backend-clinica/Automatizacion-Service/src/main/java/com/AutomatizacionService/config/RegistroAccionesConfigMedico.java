package com.AutomatizacionService.config;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.client.PacienteClient;
import com.AutomatizacionService.service.LogicaHorario.ListarHorariosService;
import com.AutomatizacionService.service.conversacionIA.AccionIARegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Configuración de acciones IA exclusivas para el rol MÉDICO.
 * Estas acciones permiten al médico consultar sus citas, gestionar horarios
 * y obtener información de los pacientes que atenderá.
 */
@Component
public class RegistroAccionesConfigMedico {

    @Autowired private CitaClient citaClient;
    @Autowired private HorarioClient horarioClient;
    @Autowired private PacienteClient pacienteClient;
    @Autowired private ListarHorariosService listarHorariosService;

    private final AccionIARegistry registry;

    public RegistroAccionesConfigMedico(AccionIARegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void registrarAcciones() {

        // 🔹 1. Listar citas
        registry.registrarAccion(
                "listar_citas_medico",
                "Muestra todas las citas programadas del médico actual",
                (medicoId, parametros) -> {
                    System.out.println("📋 Ejecutando acción: listar_citas_medico");
                    try {
                        List<Map<String, Object>> citas = citaClient.listarPorMedico(medicoId);
                        return Map.of("mensaje", "✅ Citas del médico obtenidas correctamente.", "data", Map.of("citas", citas));
                    } catch (Exception e) {
                        return Map.of("mensaje", "⚠️ No se pudieron obtener las citas: " + e.getMessage());
                    }
                }
        );

        // 🔹 2. Listar horarios
        registry.registrarAccion(
                "listar_horarios_medico",
                "Muestra los horarios del médico actual",
                (medicoId, parametros) -> {
                    System.out.println("🕒 Ejecutando acción: listar_horarios_medico");
                    try {
                        Map<String, Object> horarios = listarHorariosService.listarHorarios(medicoId, parametros);
                        return Map.of("mensaje", "✅ Horarios del médico listados correctamente.", "data", horarios.get("data"));
                    } catch (Exception e) {
                        return Map.of("mensaje", "⚠️ Error al listar horarios: " + e.getMessage());
                    }
                }
        );

        // 🔹 3. Crear horario
        registry.registrarAccion(
                "crear_horario_medico",
                "Permite al médico crear nuevos bloques de atención (automáticamente divididos por hora).",
                (medicoId, parametros) -> {
                    System.out.println("🆕 Ejecutando acción: crear_horario_medico");

                    try {
                        // Validaciones básicas
                        if (!parametros.containsKey("fechaInicio") || !parametros.containsKey("fechaFin") ||
                                !parametros.containsKey("horaInicio") || !parametros.containsKey("horaFin")) {
                            throw new IllegalArgumentException("Faltan parámetros obligatorios: fechaInicio, fechaFin, horaInicio, horaFin");
                        }

                        // Aseguramos que el ID del médico esté presente
                        parametros.put("medicoId", medicoId);

                        // Validar coherencia del rango horario
                        String horaInicioStr = parametros.get("horaInicio").toString();
                        String horaFinStr = parametros.get("horaFin").toString();
                        if (horaInicioStr.compareTo(horaFinStr) >= 0) {
                            throw new IllegalArgumentException("La horaInicio debe ser menor que horaFin.");
                        }

                        // Crear horario a través del microservicio
                        Map<String, Object> respuesta = horarioClient.crearHorario(parametros);

                        // 🚨 NUEVO: verificar si el microservicio devuelve lista o único objeto
                        Object data = respuesta.get("data");
                        List<Map<String, Object>> horariosCreados;

                        if (data instanceof List) {
                            horariosCreados = (List<Map<String, Object>>) data;
                        } else if (data instanceof Map) {
                            horariosCreados = List.of((Map<String, Object>) data);
                        } else {
                            horariosCreados = List.of();
                        }

                        int cantidad = horariosCreados.size();

                        // Mensaje dinámico para la IA
                        String mensaje;
                        if (cantidad == 0) {
                            mensaje = "⚠️ No se crearon nuevos bloques. Es posible que ya existan horarios en ese rango.";
                        } else if (cantidad == 1) {
                            Map<String, Object> unico = horariosCreados.get(0);
                            mensaje = String.format(
                                    "✅ Se creó 1 bloque horario: %s %s–%s.",
                                    unico.get("diaSemana"),
                                    unico.get("horaInicio"),
                                    unico.get("horaFin")
                            );
                        } else {
                            mensaje = String.format("✅ Se crearon %d bloques horarios de %s a %s.",
                                    cantidad, horaInicioStr, horaFinStr);
                        }

                        return Map.of(
                                "mensaje", mensaje,
                                "data", horariosCreados
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
                        return Map.of("mensaje", "❌ Error al crear horarios: " + e.getMessage());
                    }
                }
        );

        // 🔹 3C. Actualizar horario semanal completo
        registry.registrarAccion(
                "actualizar_horario_semana",
                "Permite al médico reemplazar completamente sus horarios en una semana específica con nuevos bloques de atención (por ejemplo: 'cambia mi horario desde el lunes 24 de noviembre de 9 a 17').",
                (medicoId, parametros) -> {
                    System.out.println("🗓️ Ejecutando acción: actualizar_horario_semana");

                    try {
                        // Validaciones obligatorias
                        if (!parametros.containsKey("fechaInicio") || !parametros.containsKey("fechaFin") ||
                                !parametros.containsKey("horaInicio") || !parametros.containsKey("horaFin")) {
                            throw new IllegalArgumentException("Faltan parámetros obligatorios: fechaInicio, fechaFin, horaInicio, horaFin");
                        }

                        // Convertir parámetros
                        String fechaInicioStr = parametros.get("fechaInicio").toString();
                        String fechaFinStr = parametros.get("fechaFin").toString();
                        String horaInicioStr = parametros.get("horaInicio").toString();
                        String horaFinStr = parametros.get("horaFin").toString();

                        if (horaInicioStr.compareTo(horaFinStr) >= 0) {
                            throw new IllegalArgumentException("La horaInicio debe ser menor que horaFin.");
                        }

                        // ⚠️ 1️⃣ Eliminar horarios existentes de esa semana
                        System.out.println("🧹 Eliminando horarios del médico ID " + medicoId +
                                " entre " + fechaInicioStr + " y " + fechaFinStr);

                        // Obtener todos los horarios del médico
                        List<Map<String, Object>> horariosExistentes = horarioClient.listarPorMedico(medicoId);

                        List<Long> idsAEliminar = horariosExistentes.stream()
                                .filter(h -> {
                                    LocalDate fecha = LocalDate.parse(h.get("fechaInicio").toString());
                                    return !fecha.isBefore(LocalDate.parse(fechaInicioStr)) &&
                                            !fecha.isAfter(LocalDate.parse(fechaFinStr));
                                })
                                .map(h -> Long.valueOf(h.get("id").toString()))
                                .toList();

                        for (Long id : idsAEliminar) {
                            horarioClient.eliminarHorario(id);
                            System.out.println("🗑️ Eliminado horario ID: " + id);
                        }

                        System.out.println("🧾 Total eliminados: " + idsAEliminar.size());

                        // ⚙️ 2️⃣ Crear nuevos horarios con los mismos parámetros
                        parametros.put("medicoId", medicoId);
                        Map<String, Object> resultadoCreacion = horarioClient.crearHorario(parametros);

                        Object data = resultadoCreacion.get("data");
                        List<Map<String, Object>> nuevosHorarios;

                        if (data instanceof List) {
                            nuevosHorarios = (List<Map<String, Object>>) data;
                        } else if (data instanceof Map) {
                            nuevosHorarios = List.of((Map<String, Object>) data);
                        } else {
                            nuevosHorarios = List.of();
                        }

                        int creados = nuevosHorarios.size();

                        String mensajeFinal = String.format(
                                "✅ Horarios actualizados correctamente. Se eliminaron %d bloques previos y se crearon %d nuevos, de %s a %s (del %s al %s).",
                                idsAEliminar.size(),
                                creados,
                                horaInicioStr,
                                horaFinStr,
                                fechaInicioStr,
                                fechaFinStr
                        );

                        return Map.of(
                                "mensaje", mensajeFinal,
                                "data", Map.of(
                                        "eliminados", idsAEliminar.size(),
                                        "creados", creados,
                                        "nuevosHorarios", nuevosHorarios
                                )
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
                        return Map.of("mensaje", "❌ Error al actualizar horarios semanales: " + e.getMessage());
                    }
                }
        );

        // 🔹 Acción 3B: Cambiar disponibilidad por día y hora (sin usar IDs)
        // 🔹 Acción 3B: Cambiar disponibilidad por día y hora (soporta fecha o díaSemana)
        registry.registrarAccion(
                "cambiar_disponibilidad_por_dia_hora",
                "Permite al médico cambiar su disponibilidad indicando una fecha o día específico (por ejemplo: 'el 21 de noviembre de 11 a 12 no estaré disponible' o 'los martes de 4 a 5 no atenderé').",
                (medicoId, parametros) -> {
                    System.out.println("🧭 Ejecutando acción: cambiar_disponibilidad_por_dia_hora");

                    try {
                        // Validación mínima de tiempo y disponibilidad
                        if (!parametros.containsKey("horaInicio") ||
                                !parametros.containsKey("horaFin") ||
                                !parametros.containsKey("disponible")) {
                            throw new IllegalArgumentException("Debe incluir 'horaInicio', 'horaFin' y 'disponible'");
                        }

                        String horaInicio = parametros.get("horaInicio").toString();
                        String horaFin = parametros.get("horaFin").toString();
                        boolean disponible = Boolean.parseBoolean(parametros.get("disponible").toString());

                        String diaSemana = null;
                        LocalDate fechaEspecifica = null;

                        // Detectar fecha o día de la semana
                        if (parametros.containsKey("fecha")) {
                            fechaEspecifica = LocalDate.parse(parametros.get("fecha").toString());
                            diaSemana = fechaEspecifica.getDayOfWeek().name();
                        } else if (parametros.containsKey("diaSemana")) {
                            diaSemana = parametros.get("diaSemana").toString().toUpperCase();
                        } else {
                            throw new IllegalArgumentException("Debe incluir 'fecha' o 'diaSemana' para identificar el día.");
                        }

                        // Buscar horarios del médico
                        var horarios = horarioClient.listarPorMedico(medicoId);

                        // Crear copias efectivamente finales para usarlas dentro del stream
                        final String diaSemanaFinal = diaSemana;
                        final String horaInicioFinal = horaInicio;
                        final String horaFinFinal = horaFin;

// Filtrar los bloques que coinciden con el día y la franja indicada
                        var bloquesCoincidentes = horarios.stream()
                                .filter(h -> diaSemanaFinal.equalsIgnoreCase(h.get("diaSemana").toString()))
                                .filter(h -> horaInicioFinal.equals(h.get("horaInicio").toString()) &&
                                        horaFinFinal.equals(h.get("horaFin").toString()))
                                .toList();

                        if (bloquesCoincidentes.isEmpty()) {
                            return Map.of("mensaje",
                                    "⚠️ No se encontró ningún bloque el " + (fechaEspecifica != null ? fechaEspecifica : diaSemana) +
                                            " de " + horaInicio + " a " + horaFin + ".");
                        }

                        // Actualizar disponibilidad de los bloques encontrados
                        for (var bloque : bloquesCoincidentes) {
                            Long idHorario = Long.valueOf(bloque.get("id").toString());
                            horarioClient.actualizarDisponibilidad(idHorario, disponible);
                            System.out.println("🔄 Actualizado bloque ID " + idHorario + " → disponible=" + disponible);
                        }

                        return Map.of(
                                "mensaje", String.format(
                                        "✅ Disponibilidad actualizada correctamente para %s (%s de %s a %s → disponible=%s).",
                                        fechaEspecifica != null ? fechaEspecifica : diaSemana,
                                        diaSemana, horaInicio, horaFin, disponible),
                                "data", Map.of(
                                        "fecha", fechaEspecifica,
                                        "diaSemana", diaSemana,
                                        "horaInicio", horaInicio,
                                        "horaFin", horaFin,
                                        "disponible", disponible)
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
                        return Map.of("mensaje", "❌ Error al cambiar disponibilidad: " + e.getMessage());
                    }
                }
        );



        // 🔹 4. Actualizar horario
        registry.registrarAccion(
                "actualizar_horario_medico",
                "Permite modificar los datos de un horario existente (horaInicio, horaFin, pacientesPorHora, etc.)",
                (medicoId, parametros) -> {
                    System.out.println("✏️ Ejecutando acción: actualizar_horario_medico");

                    if (!parametros.containsKey("idHorario")) {
                        throw new IllegalArgumentException("Debe incluir el parámetro 'idHorario'");
                    }

                    Long idHorario = Long.valueOf(parametros.get("idHorario").toString());
                    Map<String, Object> actualizado = horarioClient.actualizarHorario(idHorario, parametros);

                    return Map.of("mensaje", "✅ Horario actualizado correctamente.", "data", actualizado);
                }
        );

        // 🔹 5. Actualizar disponibilidad
        registry.registrarAccion(
                "actualizar_disponibilidad_horario",
                "Permite al médico cambiar la disponibilidad de un horario específico",
                (medicoId, parametros) -> {
                    System.out.println("⚙️ Ejecutando acción: actualizar_disponibilidad_horario");

                    if (!parametros.containsKey("idHorario") || !parametros.containsKey("disponible")) {
                        throw new IllegalArgumentException("Se requieren los parámetros 'idHorario' y 'disponible'");
                    }

                    Long idHorario = Long.valueOf(parametros.get("idHorario").toString());
                    Boolean disponible = Boolean.valueOf(parametros.get("disponible").toString());

                    horarioClient.actualizarDisponibilidad(idHorario, disponible);

                    return Map.of("mensaje", "✅ Disponibilidad del horario actualizada correctamente.",
                            "data", Map.of("idHorario", idHorario, "disponible", disponible));
                }
        );

        // 🔹 6. Eliminar horario
        registry.registrarAccion(
                "eliminar_horario_medico",
                "Permite al médico eliminar un bloque horario específico",
                (medicoId, parametros) -> {
                    System.out.println("🗑️ Ejecutando acción: eliminar_horario_medico");

                    if (!parametros.containsKey("idHorario")) {
                        throw new IllegalArgumentException("Debe incluir 'idHorario' para eliminar");
                    }

                    Long idHorario = Long.valueOf(parametros.get("idHorario").toString());
                    horarioClient.eliminarHorario(idHorario);
                    return Map.of("mensaje", "✅ Horario eliminado correctamente.", "data", Map.of("idHorario", idHorario));
                }
        );

        // 🔹 7. Eliminar todos los horarios del médico
        registry.registrarAccion(
                "eliminar_todos_horarios_medico",
                "Elimina todos los horarios asociados al médico actual",
                (medicoId, parametros) -> {
                    System.out.println("🧹 Ejecutando acción: eliminar_todos_horarios_medico");
                    horarioClient.eliminarPorMedico(medicoId);
                    return Map.of("mensaje", "✅ Todos los horarios del médico han sido eliminados.", "data", Map.of("medicoId", medicoId));
                }
        );

        // 🔹 8. Ver paciente por cita
        registry.registrarAccion(
                "ver_paciente_cita",
                "Muestra la información básica del paciente asociado a una cita",
                (medicoId, parametros) -> {
                    System.out.println("👤 Ejecutando acción: ver_paciente_cita");

                    Long pacienteId = null;
                    if (parametros.containsKey("pacienteId")) {
                        pacienteId = Long.valueOf(parametros.get("pacienteId").toString());
                    } else if (parametros.containsKey("citaId")) {
                        Long citaId = Long.valueOf(parametros.get("citaId").toString());
                        var citas = citaClient.listarPorMedico(medicoId);
                        var cita = citas.stream()
                                .filter(c -> c.get("id") != null && c.get("id").toString().equals(citaId.toString()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("No se encontró la cita con ID " + citaId));

                        Map<String, Object> pacienteMap = (Map<String, Object>) cita.get("paciente");
                        if (pacienteMap == null || pacienteMap.get("id") == null)
                            throw new IllegalArgumentException("No se encontró el paciente asociado a la cita " + citaId);

                        pacienteId = Long.valueOf(pacienteMap.get("id").toString());
                    } else {
                        throw new IllegalArgumentException("Debe incluir 'pacienteId' o 'citaId'");
                    }

                    Map<String, Object> paciente = pacienteClient.obtenerPacientePublico(pacienteId);
                    return Map.of("mensaje", "✅ Información del paciente obtenida correctamente.", "data", paciente);
                }
        );

        System.out.println("""
✅ Acciones IA (MÉDICO) registradas:
- listar_citas_medico
- listar_horarios_medico
- crear_horario_medico
- actualizar_horario_medico
- actualizar_disponibilidad_horario
- eliminar_horario_medico
- eliminar_todos_horarios_medico
- ver_paciente_cita
""");
    }
}