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
        System.out.println("🩺 [DEBUG] Intentando crear cita...");
        System.out.println("📦 Datos recibidos: " + cita);

        Map<String, Object> paciente = null;
        Map<String, Object> medico = null;

        try {
            paciente = pacienteClient.obtener(cita.getIdPaciente());
            medico = medicoClient.obtener(cita.getIdMedico());
        } catch (Exception e) {
            System.out.println("❌ [ERROR] Fallo al consumir microservicio externo:");
            e.printStackTrace();
        }

        System.out.println("🧩 [DEBUG] Paciente obtenido: " + paciente);
        System.out.println("🧩 [DEBUG] Médico obtenido: " + medico);

        if (paciente == null || medico == null) {
            System.out.println("🚨 [ERROR] Paciente o médico no válido. Rechazando creación.");
            throw new IllegalArgumentException("Paciente o médico no válido");
        }

        cita.setEstado("PENDIENTE");
        Cita nueva = citaRepository.save(cita);

        System.out.println("✅ [OK] Cita creada correctamente con ID: " + nueva.getId());
        return nueva;
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
        System.out.println("🩺 [DEBUG] Iniciando creación de cita con detalle...");
        System.out.println("📦 DTO recibido: " + dto);

        Cita nuevaCita = new Cita();
        nuevaCita.setFechaHora(dto.getFechaHora());
        nuevaCita.setEstado("PENDIENTE");

        // 🔹 Convertir IDs
        Long medicoId = null;
        Long pacienteId = null;
        try {
            medicoId = ((Number) dto.getMedico().get("id")).longValue();
            pacienteId = ((Number) dto.getPaciente().get("id")).longValue();
        } catch (Exception e) {
            System.out.println("⚠️ [WARN] Error al extraer IDs de médico/paciente del DTO:");
            e.printStackTrace();
        }

        nuevaCita.setIdMedico(medicoId);
        nuevaCita.setIdPaciente(pacienteId);

        System.out.println("🔍 Solicitando datos externos:");
        System.out.println("   - Médico ID: " + medicoId);
        System.out.println("   - Paciente ID: " + pacienteId);

        Map<String, Object> medico = null;
        Map<String, Object> paciente = null;

        try {
            medico = medicoClient.obtener(medicoId);
            System.out.println("✅ [OK] Respuesta médico: " + medico);
        } catch (Exception e) {
            System.out.println("❌ [ERROR] Falló la consulta al microservicio de MÉDICOS:");
            e.printStackTrace();
        }

        try {
            paciente = pacienteClient.obtener(pacienteId);
            System.out.println("✅ [OK] Respuesta paciente: " + paciente);
        } catch (Exception e) {
            System.out.println("❌ [ERROR] Falló la consulta al microservicio de PACIENTES:");
            e.printStackTrace();
        }

        if (paciente == null || medico == null) {
            System.out.println("🚨 [ERROR] Paciente o médico no válido. Datos nulos detectados.");
            throw new IllegalArgumentException("Paciente o médico no válido");
        }

        // 🔹 Persistir en base de datos
        Cita citaGuardada = citaRepository.save(nuevaCita);

        System.out.println("💾 [OK] Cita guardada con ID: " + citaGuardada.getId());

        // 🔹 Devolver DTO completo
        dto.setId(citaGuardada.getId());
        dto.setEstado(citaGuardada.getEstado());
        dto.setMedico(medico);
        dto.setPaciente(paciente);

        System.out.println("✅ [SUCCESS] Cita creada correctamente: " + dto);
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