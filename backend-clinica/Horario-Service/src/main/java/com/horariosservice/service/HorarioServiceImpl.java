package com.horariosservice.service;

import com.horariosservice.model.Horario;
import com.horariosservice.repository.HorarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class HorarioServiceImpl implements HorarioService {

    @Autowired
    private HorarioRepository horarioRepository;


    @Override
    public List<Horario> crear(Horario horario) {
        if (horario.getFechaInicio() == null || horario.getFechaFin() == null) {
            throw new IllegalArgumentException("Debe especificar fechaInicio y fechaFin");
        }

        List<Horario> subHorarios = new ArrayList<>();
        LocalDate actualFecha = horario.getFechaInicio();

        while (!actualFecha.isAfter(horario.getFechaFin())) {
            DayOfWeek dia = actualFecha.getDayOfWeek();
            if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
                actualFecha = actualFecha.plusDays(1);
                continue;
            }

            LocalTime inicio = horario.getHoraInicio();
            LocalTime fin = horario.getHoraFin();
            LocalTime actualHora = inicio;

            while (actualHora.isBefore(fin)) {
                LocalTime siguiente = actualHora.plusHours(1);
                if (siguiente.isAfter(fin)) siguiente = fin;

                if (horarioRepository.existsByMedicoIdAndDiaSemanaAndHoraInicio(
                        horario.getMedicoId(), dia.name(), actualHora)) {
                    System.out.println("⚠️ Ya existe un bloque el " + dia + " a las " + actualHora);
                    actualHora = siguiente;
                    continue;
                }

                Horario bloque = new Horario();
                bloque.setMedicoId(horario.getMedicoId());
                bloque.setDiaSemana(dia.name());
                bloque.setHoraInicio(actualHora);
                bloque.setHoraFin(siguiente);
                bloque.setFechaInicio(actualFecha);
                bloque.setFechaFin(actualFecha);
                bloque.setDisponible(true);
                bloque.setPacientesPorHora(horario.getPacientesPorHora());
                subHorarios.add(bloque);

                actualHora = siguiente;
            }
            actualFecha = actualFecha.plusDays(1);
        }

        if (subHorarios.isEmpty()) {
            throw new IllegalArgumentException("❌ No se generaron horarios válidos");
        }

        horarioRepository.saveAll(subHorarios);
        System.out.println("✅ Se crearon " + subHorarios.size() + " bloques para varios días.");

        // 🔁 Ahora devolvemos toda la lista, no solo uno
        return subHorarios;
    }


    @Override
    public List<Horario> listar() {
        return horarioRepository.findAll();
    }

    @Override
    public Horario obtener(Long id) {
        return horarioRepository.findById(id).orElse(null);
    }

    public Horario actualizar(Long id, Horario horario) {
        Horario existente = horarioRepository.findById(id).orElse(null);
        if (existente == null) return null;

        // ✅ Solo actualizamos los campos no nulos
        if (horario.getDiaSemana() != null)
            existente.setDiaSemana(horario.getDiaSemana());
        if (horario.getHoraInicio() != null)
            existente.setHoraInicio(horario.getHoraInicio());
        if (horario.getHoraFin() != null)
            existente.setHoraFin(horario.getHoraFin());


        existente.setDisponible(horario.isDisponible()); // 👈 este campo siempre viene
        if (horario.getPacientesPorHora() > 0)
            existente.setPacientesPorHora(horario.getPacientesPorHora());

        if (horario.getHoraFin().isBefore(horario.getHoraInicio())) {
            throw new IllegalArgumentException("La hora de fin no puede ser anterior a la de inicio.");
        }

        return horarioRepository.save(existente);
    }

    @Override
    public void actualizarDisponibilidad(Long id, boolean estado) {
        Horario horario = horarioRepository.findById(id).orElse(null);
        if (horario != null) {
            horario.setDisponible(estado);
            horarioRepository.save(horario);
            System.out.println("🟢 Horario " + id + " → disponible=" + estado);
        } else {
            System.out.println("⚠️ No se encontró horario con ID " + id + " para actualizar disponibilidad");
        }
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