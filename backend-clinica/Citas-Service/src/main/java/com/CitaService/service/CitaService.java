package com.CitaService.service;

import com.CitaService.model.Cita;
import com.CitaService.model.CitaDTO;
import com.CitaService.model.CitaMedicoDTO;

import java.util.List;

public interface CitaService {
    List<Cita> listar();
    CitaDTO crear(CitaDTO citaDTO);
    Cita obtener(Long id);
    List<Cita> listarPorPaciente(Long idPaciente);
    List<Cita> listarPorMedico(Long idMedico);
    CitaDTO actualizarEstado(Long id, String estado);
    void eliminar(Long id);

    CitaDTO obtenerDetalle(Long id);
    List<CitaDTO> listarDetalles();
    List<CitaDTO> listarDetallesPorPaciente(Long pacienteId);
    List<CitaDTO> listarDetallesPorMedico(Long medicoId); //

    void cancelarCitasPorPaciente(Long pacienteId);
}