package com.CitaService.service;

import com.CitaService.client.HorarioClient;
import com.CitaService.client.MedicoClient;
import com.CitaService.client.PacienteClient;
import com.CitaService.model.*;
import com.CitaService.repository.CitaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        // 🔍 Validar existencia del horario
        if (citaDTO.getIdHorario() != null) {
            try {
                List<Map<String, Object>> result = horarioClient.listarPorMedico(citaDTO.getMedico().getId());
                boolean existe = result.stream()
                        .anyMatch(h -> ((Number) h.get("id")).longValue() == citaDTO.getIdHorario());

                if (!existe)
                    throw new IllegalArgumentException("❌ El horario no pertenece al médico o no existe");
            } catch (Exception e) {
                throw new IllegalArgumentException("❌ Error verificando horario: " + e.getMessage());
            }

            // ✅ Verificar que el horario no esté ocupado en esa fecha
            List<String> estadosActivos = List.of("PENDIENTE", "CONFIRMADA", "EN_PROCESO");
            boolean ocupado = citaRepository.existsByIdHorarioAndFechaCitaAndEstadoIn(
                    citaDTO.getIdHorario(),
                    citaDTO.getFechaCita().toLocalDate(),
                    estadosActivos
            );
            if (ocupado)
                throw new IllegalArgumentException("⚠️ Ya existe una cita activa en ese horario y fecha.");
        }

        // Crear entidad base
        Cita cita = new Cita();
        cita.setFechaCreacion(LocalDateTime.now());
        cita.setFechaCita(citaDTO.getFechaCita());
        cita.setIdMedico(citaDTO.getMedico().getId());
        cita.setIdPaciente(citaDTO.getPaciente().getId());
        cita.setIdHorario(citaDTO.getIdHorario());
        cita.setEstado("PENDIENTE");

        Cita nueva = citaRepository.save(cita);

        // ✅ Actualizar disponibilidad del horario
        if (nueva.getIdHorario() != null) {
            try {
                horarioClient.actualizarDisponibilidad(nueva.getIdHorario(), false);
                System.out.println("🕒 Horario marcado como NO disponible.");
            } catch (Exception e) {
                System.out.println("⚠️ No se pudo actualizar disponibilidad del horario");
            }
        }

        // Retornar DTO enriquecido
        CitaDTO dto = CitaDTO.builder()
                .id(nueva.getId())
                .fechaCreacion(nueva.getFechaCreacion())
                .fechaCita(nueva.getFechaCita())
                .estado(nueva.getEstado())
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
    public CitaDTO actualizarEstado(Long id, String estado) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setEstado(estado);
        Cita citaActualizada = citaRepository.save(cita);

        MedicoDTO medico = medicoClient.obtener(citaActualizada.getIdMedico());
        PacienteDTO paciente = pacienteClient.obtener(citaActualizada.getIdPaciente());

        return CitaDTO.builder()
                .id(citaActualizada.getId())
                .fechaCita(citaActualizada.getFechaCita())
                .fechaCreacion(citaActualizada.getFechaCreacion())
                .estado(citaActualizada.getEstado())
                .medico(medico)
                .paciente(paciente)
                .build();
    }

    @Override
    public void eliminar(Long id) {
        Cita cita = citaRepository.findById(id).orElse(null);
        if (cita != null && cita.getIdHorario() != null) {
            try {
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
                .estado(cita.getEstado())
                .medico(medicoClient.obtener(cita.getIdMedico()))
                .paciente(pacienteClient.obtener(cita.getIdPaciente()))
                .build();
    }

    @Override
    public List<CitaDTO> listarDetalles() {
        return citaRepository.findAll().stream()
                .map(cita -> CitaDTO.builder()
                        .id(cita.getId())
                        .fechaCreacion(cita.getFechaCreacion())
                        .fechaCita(cita.getFechaCita())
                        .estado(cita.getEstado())
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
                        .estado(cita.getEstado())
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
                        .estado(cita.getEstado())
                        .paciente(pacienteClient.obtener(cita.getIdPaciente()))
                        .build()
                ).collect(Collectors.toList());
    }

    @Override
    public void cancelarCitasPorPaciente(Long pacienteId) {
        List<Cita> citas = citaRepository.findByIdPaciente(pacienteId);
        for (Cita cita : citas) {
            cita.setEstado("CANCELADA");
            citaRepository.save(cita);

            if (cita.getIdHorario() != null) {
                try {
                    horarioClient.actualizarDisponibilidad(cita.getIdHorario(), true);
                    System.out.println("🟢 Horario " + cita.getIdHorario() + " marcado como disponible nuevamente.");
                } catch (Exception e) {
                    System.out.println("⚠️ No se pudo reabrir disponibilidad del horario " + cita.getIdHorario());
                }
            }
        }
    }
}
