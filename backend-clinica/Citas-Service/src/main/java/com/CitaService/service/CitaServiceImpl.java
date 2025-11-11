package com.CitaService.service;

import com.CitaService.client.HorarioClient;
import com.CitaService.client.MedicoClient;
import com.CitaService.client.PacienteClient;
import com.CitaService.model.*;
import com.CitaService.repository.CitaRepository;
import com.CitaService.repository.EstadoCitaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CitaServiceImpl implements CitaService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private EstadoCitaRepository estadoCitaRepository;

    @Autowired
    private MedicoClient medicoClient;

    @Autowired
    private PacienteClient pacienteClient;

    @Autowired
    private HorarioClient horarioClient;

    @Override
    public List<Cita> listar() {
        return citaRepository.findAll();
    }

    @Override
    public CitaDTO crear(CitaDTO citaDTO) {
        System.out.println("🩺 [DEBUG] Intentando crear cita...");
        System.out.println("📦 Datos recibidos: " + citaDTO);

        if (citaDTO.getMedico() == null || citaDTO.getMedico().getId() == null)
            throw new IllegalArgumentException("Debe especificar un médico válido");
        if (citaDTO.getPaciente() == null || citaDTO.getPaciente().getId() == null)
            throw new IllegalArgumentException("Debe especificar un paciente válido");

        // 🔍 Validar médico y paciente en microservicios
        PacienteDTO paciente = pacienteClient.obtener(citaDTO.getPaciente().getId());
        MedicoDTO medico = medicoClient.obtener(citaDTO.getMedico().getId());

        if (paciente == null || medico == null)
            throw new IllegalArgumentException("Paciente o médico no válido");

        // 🔍 Validar existencia del horario y obtener hora real
        Map<String, Object> horarioSeleccionado = null;
        if (citaDTO.getIdHorario() != null) {
            try {
                List<Map<String, Object>> result = horarioClient.listarPorMedico(citaDTO.getMedico().getId());
                horarioSeleccionado = result.stream()
                        .filter(h -> ((Number) h.get("id")).longValue() == citaDTO.getIdHorario())
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("❌ El horario no pertenece al médico o no existe"));
            } catch (Exception e) {
                throw new IllegalArgumentException("❌ Error verificando horario: " + e.getMessage());
            }

            // ✅ Verificar que el horario no esté ocupado en esa fecha
            List<String> estadosActivos = List.of("PENDIENTE", "CONFIRMADA", "EN_PROCESO");
            boolean ocupado = citaRepository.existsByIdHorarioAndFechaCitaAndEstado_CodigoIn(
                    citaDTO.getIdHorario(),
                    citaDTO.getFechaCita(),
                    estadosActivos
            );
            if (ocupado)
                throw new IllegalArgumentException("⚠️ Ya existe una cita activa en ese horario y fecha.");

            // 🕓 Ajustar fechaCita para que coincida con la horaInicio del horario
            if (horarioSeleccionado != null && horarioSeleccionado.get("horaInicio") != null) {
                String horaInicioStr = horarioSeleccionado.get("horaInicio").toString();
                LocalDateTime fechaOriginal = citaDTO.getFechaCita(); // puede venir de la IA
                LocalDateTime fechaCorregida = LocalDateTime.of(
                        fechaOriginal.toLocalDate(),
                        java.time.LocalTime.parse(horaInicioStr)
                );
                citaDTO.setFechaCita(fechaCorregida);
                System.out.println("🕒 [AJUSTE] Fecha de cita corregida según horario: " + fechaCorregida);
            }
        }

        // 🧱 Obtener el estado (por código o usar "PENDIENTE" por defecto)
        EstadoCita estado;
        if (citaDTO.getEstado() != null && !citaDTO.getEstado().isBlank()) {
            estado = estadoCitaRepository.findByCodigo(citaDTO.getEstado().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Estado no válido: " + citaDTO.getEstado()));
        } else {
            estado = estadoCitaRepository.findByCodigo("PENDIENTE")
                    .orElseThrow(() -> new IllegalStateException("No existe el estado PENDIENTE"));
        }

        // Crear entidad base
        Cita cita = new Cita();
        cita.setFechaCreacion(LocalDateTime.now());
        cita.setFechaCita(citaDTO.getFechaCita());
        cita.setIdMedico(citaDTO.getMedico().getId());
        cita.setIdPaciente(citaDTO.getPaciente().getId());
        cita.setIdHorario(citaDTO.getIdHorario());
        cita.setEstado(estado);

        Cita nueva = citaRepository.save(cita);

        // ✅ Actualizar disponibilidad del horario
        // ✅ Actualizar disponibilidad del horario solo si el cupo está lleno
        // ✅ Verificar si el horario debe quedar ocupado según cupo
        if (nueva.getIdHorario() != null) {
            try {
                List<Map<String, Object>> horarios = horarioClient.listarPorMedico(nueva.getIdMedico());
                Map<String, Object> horario = horarios.stream()
                        .filter(h -> ((Number) h.get("id")).longValue() == nueva.getIdHorario())
                        .findFirst()
                        .orElse(null);

                if (horario != null) {
                    int pacientesPorHora = ((Number) horario.get("pacientesPorHora")).intValue();

                    List<String> estadosActivos = List.of("PENDIENTE", "CONFIRMADA", "EN_PROCESO", "COMPLETADA");
                    long citasActivas = citaRepository.findAll().stream()
                            .filter(c -> c.getIdHorario() != null
                                    && c.getIdHorario().equals(nueva.getIdHorario())
                                    && c.getEstado() != null
                                    && estadosActivos.contains(c.getEstado().getCodigo()))
                            .count();

                    if (citasActivas >= pacientesPorHora) {
                        System.out.println("🌐 Llamando a HorarioClient para actualizar disponibilidad...");
                        horarioClient.actualizarDisponibilidad(nueva.getIdHorario(), false);
                        System.out.println("🕒 Cupo completo → Horario " + nueva.getIdHorario() + " marcado como NO disponible (" + citasActivas + "/" + pacientesPorHora + ")");
                    } else {
                        System.out.println("🟢 Cupo disponible (" + citasActivas + "/" + pacientesPorHora + ")");
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ No se pudo actualizar disponibilidad del horario: " + e.getMessage());
            }
        }


        // Retornar DTO enriquecido
        CitaDTO dto = CitaDTO.builder()
                .id(nueva.getId())
                .fechaCreacion(nueva.getFechaCreacion())
                .fechaCita(nueva.getFechaCita())
                .estado(nueva.getEstado() != null ? nueva.getEstado().getCodigo() : "DESCONOCIDO")
                .medico(medico)
                .paciente(paciente)
                .idHorario(nueva.getIdHorario())
                .build();

        System.out.println("✅ [SUCCESS] Cita creada correctamente: " + dto);
        return dto;
    }

    @Override
    public Cita obtener(Long id) {
        return citaRepository.findById(id).orElse(null);
    }

    @Override
    public List<Cita> listarPorPaciente(Long idPaciente) {
        return citaRepository.findByIdPaciente(idPaciente);
    }

    @Override
    public List<Cita> listarPorMedico(Long idMedico) {
        return citaRepository.findByIdMedico(idMedico);
    }

    @Override
    public CitaDTO actualizarEstado(Long id, String nuevoEstado) {
        System.out.println("🔄 [INFO] Solicitando cambio de estado para cita ID: " + id + " → " + nuevoEstado);

        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        EstadoCita estado = estadoCitaRepository.findByCodigo(nuevoEstado.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Estado no válido: " + nuevoEstado));

        cita.setEstado(estado);
        Cita citaActualizada = citaRepository.saveAndFlush(cita); // 👈 flush inmediato para reflejar cambios
        System.out.println("✅ [SUCCESS] Estado actualizado a: " + estado.getCodigo());

        if (citaActualizada.getIdHorario() != null) {
            try {
                List<Map<String, Object>> horarios = horarioClient.listarPorMedico(citaActualizada.getIdMedico());
                Map<String, Object> horario = horarios.stream()
                        .filter(h -> ((Number) h.get("id")).longValue() == citaActualizada.getIdHorario())
                        .findFirst()
                        .orElse(null);

                if (horario != null) {
                    int pacientesPorHora = ((Number) horario.get("pacientesPorHora")).intValue();
                    List<String> estadosActivos = List.of("PENDIENTE", "CONFIRMADA", "EN_PROCESO", "COMPLETADA");

                    long cantidadCitasActivas = citaRepository.findAll().stream()
                            .filter(c -> c.getIdHorario() != null
                                    && c.getIdHorario().equals(citaActualizada.getIdHorario())
                                    && c.getEstado() != null
                                    && estadosActivos.contains(c.getEstado().getCodigo()))
                            .count();

                    System.out.println("📊 Citas activas para horario " + citaActualizada.getIdHorario() + ": "
                            + cantidadCitasActivas + "/" + pacientesPorHora);

                    // 🟢 Caso 1: CANCELADA → liberar
                    if ("CANCELADA".equalsIgnoreCase(estado.getCodigo())) {
                        horarioClient.actualizarDisponibilidad(citaActualizada.getIdHorario(), true);
                        System.out.println("🟢 Horario liberado tras cancelación.");
                    }

                    // 🔴 Caso 2: CONFIRMADA o EN_PROCESO → ocupar
                    else if (List.of("PENDIENTE","CONFIRMADA", "EN_PROCESO").contains(estado.getCodigo().toUpperCase())) {
                        if (cantidadCitasActivas >= pacientesPorHora) {
                            horarioClient.actualizarDisponibilidad(citaActualizada.getIdHorario(), false);
                            System.out.println("🔴 Horario marcado como NO disponible tras confirmación o proceso.");
                        } else {
                            // Si aún hay espacio, igual asegúrate de mantener el estado coherente
                            System.out.println("🟢 Horario sigue disponible (cupos: "
                                    + cantidadCitasActivas + "/" + pacientesPorHora + ")");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Error al actualizar disponibilidad tras cambio de estado: " + e.getMessage());
            }
        }

        // 🔍 Enriquecer con datos de médico y paciente
        MedicoDTO medico = medicoClient.obtener(citaActualizada.getIdMedico());
        PacienteDTO paciente = pacienteClient.obtener(citaActualizada.getIdPaciente());

        return CitaDTO.builder()
                .id(citaActualizada.getId())
                .fechaCita(citaActualizada.getFechaCita())
                .fechaCreacion(citaActualizada.getFechaCreacion())
                .estado(estado.getCodigo())
                .medico(medico)
                .paciente(paciente)
                .idHorario(citaActualizada.getIdHorario())
                .build();
    }


    @Override
    public void eliminar(Long id) {
        Cita cita = citaRepository.findById(id).orElse(null);
        if (cita != null && cita.getIdHorario() != null) {
            try {
                System.out.println("🌐 Llamando a HorarioClient para actualizar disponibilidad...");

                horarioClient.actualizarDisponibilidad(cita.getIdHorario(), true);
                System.out.println("🟢 Horario liberado tras eliminación de cita.");
            } catch (Exception e) {
                System.out.println("⚠️ No se pudo liberar horario tras eliminación de cita.");
            }
        }
        citaRepository.deleteById(id);
    }

    @Override
    public CitaDTO obtenerDetalle(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        return CitaDTO.builder()
                .id(cita.getId())
                .fechaCreacion(cita.getFechaCreacion())
                .fechaCita(cita.getFechaCita())
                .estado(cita.getEstado() != null ? cita.getEstado().getCodigo() : "DESCONOCIDO")
                .medico(medicoClient.obtener(cita.getIdMedico()))
                .paciente(pacienteClient.obtener(cita.getIdPaciente()))
                .idHorario(cita.getIdHorario())
                .build();
    }



    @Override
    public CitaDTO actualizarDetalle(Long id, CitaDTO citaDTO) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        // ✅ liberar horario anterior si cambia
        if (citaDTO.getIdHorario() != null && !citaDTO.getIdHorario().equals(cita.getIdHorario())) {
            try {
                horarioClient.actualizarDisponibilidad(cita.getIdHorario(), true);
                System.out.println("🟢 Horario anterior liberado: " + cita.getIdHorario());
            } catch (Exception e) {
                System.out.println("⚠️ No se pudo liberar horario anterior: " + e.getMessage());
            }
        }

        // 🕓 actualizar nuevos campos
        cita.setFechaCita(citaDTO.getFechaCita());
        cita.setIdHorario(citaDTO.getIdHorario());
        citaRepository.save(cita);

        // ✅ bloquear nuevo horario
        try {
            horarioClient.actualizarDisponibilidad(citaDTO.getIdHorario(), false);
            System.out.println("🔴 Nuevo horario bloqueado: " + citaDTO.getIdHorario());
        } catch (Exception e) {
            System.out.println("⚠️ No se pudo bloquear nuevo horario: " + e.getMessage());
        }

        // 🔁 retornar DTO actualizado
        return CitaDTO.builder()
                .id(cita.getId())
                .fechaCita(cita.getFechaCita())
                .fechaCreacion(cita.getFechaCreacion())
                .estado(cita.getEstado().getCodigo())
                .medico(medicoClient.obtener(cita.getIdMedico()))
                .paciente(pacienteClient.obtener(cita.getIdPaciente()))
                .idHorario(cita.getIdHorario())
                .build();
    }

    @Override
    public List<CitaDTO> listarDetalles() {
        return citaRepository.findAll().stream()
                .map(cita -> CitaDTO.builder()
                        .id(cita.getId())
                        .fechaCreacion(cita.getFechaCreacion())
                        .fechaCita(cita.getFechaCita())
                        .estado(cita.getEstado() != null ? cita.getEstado().getCodigo() : "DESCONOCIDO")
                        .medico(medicoClient.obtener(cita.getIdMedico()))
                        .paciente(pacienteClient.obtener(cita.getIdPaciente()))
                        .build()
                ).collect(Collectors.toList());
    }

    @Override
    public List<CitaDTO> listarDetallesPorPaciente(Long pacienteId) {
        return citaRepository.findByIdPaciente(pacienteId).stream()
                .map(cita -> CitaDTO.builder()
                        .id(cita.getId())
                        .fechaCreacion(cita.getFechaCreacion())
                        .fechaCita(cita.getFechaCita())
                        .estado(cita.getEstado() != null ? cita.getEstado().getCodigo() : "DESCONOCIDO")
                        .medico(medicoClient.obtener(cita.getIdMedico()))
                        .paciente(pacienteClient.obtener(cita.getIdPaciente()))
                        .build()
                ).collect(Collectors.toList());
    }

    @Override
    public List<CitaDTO> listarDetallesPorMedico(Long medicoId) {
        return citaRepository.findByIdMedico(medicoId).stream()
                .map(cita -> CitaDTO.builder()
                        .id(cita.getId())
                        .fechaCreacion(cita.getFechaCreacion())
                        .fechaCita(cita.getFechaCita())
                        .estado(cita.getEstado() != null ? cita.getEstado().getCodigo() : "DESCONOCIDO")
                        .paciente(pacienteClient.obtener(cita.getIdPaciente()))
                        .build()
                ).collect(Collectors.toList());
    }

    @Override
    public void cancelarCitasPorPaciente(Long pacienteId) {
        List<Cita> citas = citaRepository.findByIdPaciente(pacienteId);
        EstadoCita cancelada = estadoCitaRepository.findByCodigo("CANCELADA")
                .orElseThrow(() -> new IllegalStateException("No existe el estado CANCELADA"));

        for (Cita cita : citas) {
            cita.setEstado(cancelada);
            citaRepository.save(cita);

            if (cita.getIdHorario() != null) {
                try {
                    System.out.println("🌐 Llamando a HorarioClient para actualizar disponibilidad...");

                    horarioClient.actualizarDisponibilidad(cita.getIdHorario(), true);
                    System.out.println("🟢 Horario " + cita.getIdHorario() + " marcado como disponible nuevamente.");
                } catch (Exception e) {
                    System.out.println("⚠️ No se pudo reabrir disponibilidad del horario " + cita.getIdHorario());
                }
            }
        }
    }
}
