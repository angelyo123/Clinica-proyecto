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
    public Cita actualizarEstado(Long id, String estado) {
        Cita cita = citaRepository.findById(id).orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        cita.setEstado(estado);
        return citaRepository.save(cita);
    }

    @Override
    public void eliminar(Long id) {
        citaRepository.deleteById(id);
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


}