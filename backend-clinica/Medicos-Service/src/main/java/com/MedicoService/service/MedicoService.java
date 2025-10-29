package com.MedicoService.service;


import com.MedicoService.model.Medico;

import java.util.List;

public interface MedicoService {
    List<Medico> listar();
    Medico obtenerPorId(Long id);
    Medico crear(Medico medico);
    Medico actualizar(Long id, Medico medico);
    void eliminar(Long id);
    List<Medico> listarPorEspecialidad(String especialidad);
}
