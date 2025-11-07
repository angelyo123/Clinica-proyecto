package com.horariosservice.service;

import com.horariosservice.model.Horario;

import java.util.List;

public interface HorarioService {
    List<Horario> listar();
    Horario obtener(Long id);
    List<Horario> crear(Horario horario);
    Horario actualizar(Long id, Horario horario);
    void eliminar(Long id);
    List<Horario> listarPorMedico(Long medicoId);
    // 🧨 Nuevo: eliminar todos los horarios
    void eliminarTodos();
    // 🧩 (Opcional) eliminar horarios de un médico específico
    void eliminarPorMedico(Long medicoId);
    public void actualizarDisponibilidad(Long id, boolean estado);
}