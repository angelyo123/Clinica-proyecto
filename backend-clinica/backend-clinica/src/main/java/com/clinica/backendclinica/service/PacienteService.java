package com.clinica.backendclinica.service;

import com.clinica.backendclinica.model.Paciente;

import java.util.List;

public interface PacienteService {

    List<Paciente> listar();
    Paciente ObtenerId(Long id);
    Paciente Crear(Paciente paciente);
    Paciente Actualizar(Long id, Paciente paciente);
    void Eliminar(Long id);
    Paciente guardarPaciente(Paciente paciente);
}
