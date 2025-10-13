package com.PacienteService.service;

import com.PacienteService.model.Paciente;
import java.util.List;

public interface PacienteService {

    List<Paciente> listar();
    Paciente ObtenerId(Long id);
    Paciente Crear(Paciente paciente);
    Paciente Actualizar(Long id, Paciente paciente);
    void Eliminar(Long id);
    Paciente guardarPaciente(Paciente paciente);
    Paciente obtenerPorUsuario(String username);
}
