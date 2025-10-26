package com.CitaService.service;

import com.CitaService.model.Cita;
import com.CitaService.model.CitaDTO;
import com.CitaService.model.CitaMedicoDTO;

import java.util.List;

public interface CitaService {
    List<Cita> listar();
    Cita crear(Cita cita);
    Cita obtener(Long id);
    List<Cita> listarPorPaciente(Long idPaciente);
    List<Cita> listarPorMedico(Long idMedico);
    Cita actualizarEstado(Long id, String estado);
    void eliminar(Long id);

    CitaDTO obtenerDetalle(Long id);
    List<CitaMedicoDTO> listarCitasPorMedicoConPacientes(Long medicoId);
    List<CitaDTO> listarDetalles();
    CitaDTO crearDetalle(CitaDTO citaDTO);
    CitaDTO actualizarDetalle(Long id, CitaDTO citaDTO);
    List<CitaDTO> listarDetallesPorPaciente(Long pacienteId);
    List<CitaMedicoDTO> listarDetallesPorMedico(Long medicoId); //
}