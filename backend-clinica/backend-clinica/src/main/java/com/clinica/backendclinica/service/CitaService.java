package com.clinica.backendclinica.service;

import com.clinica.backendclinica.model.Cita;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CitaService {

    Cita crearCita(Cita cita);
    Cita actualizarCita(Cita cita, Long id);
    void eliminarCita(Long id);
    List<Cita> listarCitas(Authentication auth); // mantiene compatibilidad
    List<Cita> listarPorPaciente(Long pacienteId); // 👈 nuevo
    List<Cita> listarPorMedico(Long medicoId);    // 👈 nuevo
    List<Cita> listarTodas();                     // 👈 opcional (admin)
    Cita buscarCita(Long id);
}