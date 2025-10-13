package com.horariosservice.service;

import com.horariosservice.model.Horario;

import java.util.List;

public interface HorarioService {
    List<Horario> listar();
    Horario obtener(Long id);
    Horario crear(Horario horario);
    Horario actualizar(Long id, Horario horario);
    void eliminar(Long id);
    List<Horario> listarPorMedico(Long medicoId);
}