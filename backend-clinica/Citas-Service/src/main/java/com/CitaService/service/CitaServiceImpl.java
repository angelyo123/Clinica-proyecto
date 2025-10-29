package com.CitaService.service;

import com.CitaService.client.MedicoClient;
import com.CitaService.client.PacienteClient;
import com.CitaService.model.*;
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



        try {
            System.out.println("🧠 ID Paciente recibido: " + cita.getIdPaciente());
            System.out.println("🧠 ID Médico recibido: " + cita.getIdMedico());

            PacienteDTO paciente = pacienteClient.obtener(cita.getIdPaciente());
            MedicoDTO medico = medicoClient.obtener(cita.getIdMedico());

            System.out.println("✅ Paciente respuesta: " + paciente);
            System.out.println("✅ Médico respuesta: " + medico);

            if (paciente == null || medico == null) {
                throw new IllegalArgumentException("Paciente o médico no válido");
            }

            cita.setEstado("PENDIENTE");
            return citaRepository.save(cita);
        } catch (Exception e) {
            System.err.println("❌ Error al validar entidades externas: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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