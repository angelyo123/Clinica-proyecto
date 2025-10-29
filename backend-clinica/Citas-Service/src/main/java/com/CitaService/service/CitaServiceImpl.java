package com.CitaService.service;

import com.CitaService.client.MedicoClient;
import com.CitaService.client.PacienteClient;
import com.CitaService.model.Cita;
import com.CitaService.model.CitaDTO;
import com.CitaService.model.CitaMedicoDTO;
import com.CitaService.repository.CitaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public List<Cita> listar() {
        return citaRepository.findAll();
    }

    @Override
    public Cita crear(Cita cita) {
        Map<String, Object> paciente = pacienteClient.obtener(cita.getIdPaciente());
        Map<String, Object> medico = medicoClient.obtener(cita.getIdMedico());
        if (paciente == null || medico == null) {
            throw new IllegalArgumentException("Paciente o médico no válido");
        }
        cita.setEstado("PENDIENTE");
        return citaRepository.save(cita);
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
        // 1. Encuentra la cita
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        // 2. Actualiza el estado y guarda
        cita.setEstado(estado);
        Cita citaActualizada = citaRepository.save(cita);

        // 3. Obtén los detalles del médico y paciente
        Map<String, Object> medico = medicoClient.obtener(citaActualizada.getIdMedico());
        Map<String, Object> paciente = pacienteClient.obtener(citaActualizada.getIdPaciente());

        // 4. Construye y retorna el CitaDTO
        CitaDTO dto = new CitaDTO();
        dto.setId(citaActualizada.getId());
        dto.setFechaHora(citaActualizada.getFechaHora());
        dto.setEstado(citaActualizada.getEstado());
        dto.setMedico(medico);
        dto.setPaciente(paciente);

        return dto;
    }

    @Override
    public void eliminar(Long id) {
        citaRepository.deleteById(id);
    }
    @Override
    @Transactional
    public void eliminarPorPaciente(Long idPaciente) {
        System.out.println("🗑️ Eliminando todas las citas del paciente ID: " + idPaciente);
        citaRepository.deleteByIdPaciente(idPaciente);
        System.out.println("✅ Citas eliminadas");
    }

    @Override
    public CitaDTO obtenerDetalle(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        CitaDTO dto = new CitaDTO();
        dto.setId(cita.getId());
        dto.setFechaHora(cita.getFechaHora());
        dto.setEstado(cita.getEstado());
        dto.setMedico(medicoClient.obtener(cita.getIdMedico()));
        dto.setPaciente(pacienteClient.obtener(cita.getIdPaciente()));
        return dto;
    }

    public List<CitaMedicoDTO> listarCitasPorMedicoConPacientes(Long medicoId) {
        List<Cita> citas = citaRepository.findByIdMedico(medicoId);

        return citas.stream().map(c -> {
            CitaMedicoDTO dto = new CitaMedicoDTO();
            dto.setId(c.getId());
            dto.setFechaHora(c.getFechaHora());
            dto.setEstado(c.getEstado());
            dto.setPaciente(pacienteClient.obtener(c.getIdPaciente()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CitaDTO> listarDetalles() {
        List<Cita> citasDeLaBD = citaRepository.findAll();

        return citasDeLaBD.stream()
                .map(cita -> {
                    CitaDTO dto = new CitaDTO();
                    dto.setId(cita.getId());
                    dto.setFechaHora(cita.getFechaHora());
                    dto.setEstado(cita.getEstado());
                    dto.setMedico(medicoClient.obtener(cita.getIdMedico()));
                    dto.setPaciente(pacienteClient.obtener(cita.getIdPaciente()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CitaDTO crearDetalle(CitaDTO dto) {
        Cita nuevaCita = new Cita();
        nuevaCita.setFechaHora(dto.getFechaHora());
        nuevaCita.setEstado("PENDIENTE");

        Long medicoId = ((Number) dto.getMedico().get("id")).longValue();
        Long pacienteId = ((Number) dto.getPaciente().get("id")).longValue();

        nuevaCita.setIdMedico(medicoId);
        nuevaCita.setIdPaciente(pacienteId);

        Map<String, Object> medico = medicoClient.obtener(medicoId);
        Map<String, Object> paciente = pacienteClient.obtener(pacienteId);
        if (paciente == null || medico == null) {
            throw new IllegalArgumentException("Paciente o médico no válido");
        }

        Cita citaGuardada = citaRepository.save(nuevaCita);

        dto.setId(citaGuardada.getId());
        dto.setEstado(citaGuardada.getEstado());
        dto.setMedico(medico);
        dto.setPaciente(paciente);
        return dto;
    }

    @Override
    public CitaDTO actualizarDetalle(Long id, CitaDTO dto) {
        Cita citaExistente = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada con id: " + id));

        Long medicoId = ((Number) dto.getMedico().get("id")).longValue();
        Long pacienteId = ((Number) dto.getPaciente().get("id")).longValue();

        Map<String, Object> medico = medicoClient.obtener(medicoId);
        Map<String, Object> paciente = pacienteClient.obtener(pacienteId);
        if (paciente == null || medico == null) {
            throw new IllegalArgumentException("Paciente o médico no válido");
        }

        citaExistente.setFechaHora(dto.getFechaHora());
        citaExistente.setEstado(dto.getEstado());
        citaExistente.setIdMedico(medicoId);
        citaExistente.setIdPaciente(pacienteId);

        citaRepository.save(citaExistente);

        dto.setId(id);
        dto.setMedico(medico);
        dto.setPaciente(paciente);

        return dto;
    }

    @Override
    public List<CitaDTO> listarDetallesPorPaciente(Long pacienteId) {
        List<Cita> citasDelPaciente = citaRepository.findByIdPaciente(pacienteId);

        return citasDelPaciente.stream()
                .map(cita -> {
                    CitaDTO dto = new CitaDTO();
                    dto.setId(cita.getId());
                    dto.setFechaHora(cita.getFechaHora());
                    dto.setEstado(cita.getEstado());
                    dto.setMedico(medicoClient.obtener(cita.getIdMedico()));
                    dto.setPaciente(pacienteClient.obtener(cita.getIdPaciente()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CitaDTO> listarDetallesPorMedico(Long medicoId) {
        List<Cita> citasDelMedico = citaRepository.findByIdMedico(medicoId);

        return citasDelMedico.stream()
                .map(cita -> {
                    CitaDTO dto = new CitaDTO();
                    dto.setId(cita.getId());
                    dto.setFechaHora(cita.getFechaHora());
                    dto.setEstado(cita.getEstado());
                    dto.setPaciente(pacienteClient.obtener(cita.getIdPaciente()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}