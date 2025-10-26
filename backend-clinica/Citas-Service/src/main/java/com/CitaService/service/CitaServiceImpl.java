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

    @Override
    public List<CitaDTO> listarDetalles() {
        // 1. Obtiene todas las citas (crudas) de la BD
        List<Cita> citasDeLaBD = citaRepository.findAll();

        // 2. Itera sobre la lista y convierte cada Cita a CitaDTO
        return citasDeLaBD.stream()
                .map(cita -> {
                    // 3. Copiamos la lógica de tu 'obtenerDetalle'
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
        // 1. "Desenvolver" el DTO para crear la entidad plana
        Cita nuevaCita = new Cita();
        nuevaCita.setFechaHora(dto.getFechaHora());
        nuevaCita.setEstado("PENDIENTE"); // O usa dto.getEstado() si ya lo envías

        // 2. Extraer los IDs de los Maps (o de los objetos anidados)
        // (Esto asume que el DTO envía { "id": 1 } en el Map/objeto)
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
        // 1. Busca la cita existente en la BD
        Cita citaExistente = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada con id: " + id));

        // 2. Extrae los IDs del DTO
        Long medicoId = ((Number) dto.getMedico().get("id")).longValue();
        Long pacienteId = ((Number) dto.getPaciente().get("id")).longValue();

        // 3. (Opcional) Valida que los nuevos IDs existan
        Map<String, Object> medico = medicoClient.obtener(medicoId);
        Map<String, Object> paciente = pacienteClient.obtener(pacienteId);
        if (paciente == null || medico == null) {
            throw new IllegalArgumentException("Paciente o médico no válido");
        }

        // 4. Actualiza la entidad plana
        citaExistente.setFechaHora(dto.getFechaHora());
        citaExistente.setEstado(dto.getEstado());
        citaExistente.setIdMedico(medicoId);
        citaExistente.setIdPaciente(pacienteId);

        // 5. Guarda la entidad actualizada
        citaRepository.save(citaExistente);

        // 6. Devuelve el DTO actualizado y enriquecido
        // (Reutilizamos los datos que ya buscamos)
        dto.setId(id);
        dto.setMedico(medico);
        dto.setPaciente(paciente);

        return dto;
    }

    @Override
    public List<CitaDTO> listarDetallesPorPaciente(Long pacienteId) {
        // 1. Busca las citas crudas solo para este paciente
        List<Cita> citasDelPaciente = citaRepository.findByIdPaciente(pacienteId);

        // 2. Convierte cada cita a CitaDTO (reutiliza la lógica existente)
        return citasDelPaciente.stream()
                .map(cita -> {
                    // Copiamos la lógica de 'obtenerDetalle' o 'listarDetalles'
                    CitaDTO dto = new CitaDTO();
                    dto.setId(cita.getId());
                    dto.setFechaHora(cita.getFechaHora());
                    dto.setEstado(cita.getEstado());
                    // Llamamos a los clients para enriquecer
                    dto.setMedico(medicoClient.obtener(cita.getIdMedico()));
                    // Para la vista del paciente, su propio detalle es menos crucial,
                    // pero el DTO lo requiere, así que lo incluimos.
                    dto.setPaciente(pacienteClient.obtener(cita.getIdPaciente()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CitaMedicoDTO> listarDetallesPorMedico(Long medicoId) {
        // 1. Busca las citas crudas solo para este médico
        List<Cita> citasDelMedico = citaRepository.findByIdMedico(medicoId);

        // 2. Convierte cada cita a CitaMedicoDTO (reutiliza la lógica existente)
        return citasDelMedico.stream()
                .map(cita -> {
                    // Copiamos la lógica de 'listarCitasPorMedicoConPacientes'
                    CitaMedicoDTO dto = new CitaMedicoDTO();
                    dto.setId(cita.getId());
                    dto.setFechaHora(cita.getFechaHora());
                    dto.setEstado(cita.getEstado());
                    // Llamamos solo al PacienteClient
                    dto.setPaciente(pacienteClient.obtener(cita.getIdPaciente()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}