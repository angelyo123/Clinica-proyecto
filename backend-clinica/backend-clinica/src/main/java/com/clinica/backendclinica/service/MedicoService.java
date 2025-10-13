package com.clinica.backendclinica.service;

import com.clinica.backendclinica.model.Medico;

import java.util.List;

public interface MedicoService {
    List<Medico> listar();
    Medico ObtenerPorId(Long id);
    Medico Crear(Medico medico);
    Medico Actualizar(Long id, Medico medico);
    void Eliminar(Long id);
}
