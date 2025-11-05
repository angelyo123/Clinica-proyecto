package com.horariosservice.service;

import com.horariosservice.model.Horario;
import com.horariosservice.repository.HorarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class HorarioServiceImpl implements HorarioService {

    @Autowired
    private HorarioRepository horarioRepository;

    @Override
    public List<Horario> listar() {
        return horarioRepository.findAll();
    }

    @Override
    public Horario obtener(Long id) {
        return horarioRepository.findById(id).orElse(null);
    }

    @Override
    public Horario crear(Horario horario) {
        List<Horario> subHorarios = new ArrayList<>();
        LocalTime inicio = horario.getHoraInicio();
        LocalTime fin = horario.getHoraFin();
        LocalTime actual = inicio;

        while (actual.isBefore(fin)) {
            LocalTime siguiente = actual.plusHours(1);
            if (siguiente.isAfter(fin)) siguiente = fin;

            // 🚫 Validación: evitar duplicados
            if (horarioRepository.existsByMedicoIdAndDiaSemanaAndHoraInicio(
                    horario.getMedicoId(), horario.getDiaSemana(), actual)) {
                System.out.println("⚠️ Ya existe un horario a las " + actual + " para este médico y día.");
                actual = siguiente;
                continue; // salta la creación de ese bloque
            }

            Horario bloque = new Horario();
            bloque.setDiaSemana(horario.getDiaSemana());
            bloque.setMedicoId(horario.getMedicoId());
            bloque.setHoraInicio(actual);
            bloque.setHoraFin(siguiente);
            bloque.setDisponible(true);
            bloque.setPacientesPorHora(horario.getPacientesPorHora());

            // 🗓️ Nuevo: asignar fechas si se proporcionan
            bloque.setFechaInicio(horario.getFechaInicio());
            bloque.setFechaFin(horario.getFechaFin());

            subHorarios.add(bloque);
            actual = siguiente;
        }

        if (subHorarios.isEmpty()) {
            throw new IllegalArgumentException("❌ Todos los horarios ya estaban registrados");
        }

        horarioRepository.saveAll(subHorarios);
        System.out.println("✅ Se crearon " + subHorarios.size() + " bloques de horario para el médico " + horario.getMedicoId());
        return subHorarios.get(0); // devuelve el primero solo para confirmar creación
    }

    @Override
    public Horario actualizar(Long id, Horario horario) {
        Horario existente = horarioRepository.findById(id).orElse(null);
        if (existente == null) return null;

        existente.setDiaSemana(horario.getDiaSemana());
        existente.setHoraInicio(horario.getHoraInicio());
        existente.setHoraFin(horario.getHoraFin());
        existente.setDisponible(horario.isDisponible());
        existente.setPacientesPorHora(horario.getPacientesPorHora());
        return horarioRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        horarioRepository.deleteById(id);
    }

    @Override
    public List<Horario> listarPorMedico(Long idMedico) {
        System.out.println("🔍 Buscando horarios del médico ID: " + idMedico);
        List<Horario> result = horarioRepository.findByMedicoId(idMedico);
        System.out.println("✅ Horarios encontrados: " + result.size());
        return result;
    }

    @Override
    public void eliminarTodos() {
        long total = horarioRepository.count(); // cuenta cuántos hay antes de borrar
        horarioRepository.deleteAll(); // borra todos los registros
        System.out.println("🗑️ Se eliminaron todos los horarios (" + total + " registros).");
    }

    @Override
    public void eliminarPorMedico(Long medicoId) {
        long total = horarioRepository.findByMedicoId(medicoId).size();
        horarioRepository.deleteByMedicoId(medicoId);
        System.out.println("🗑️ Se eliminaron " + total + " horarios del médico con ID " + medicoId);
    }

}