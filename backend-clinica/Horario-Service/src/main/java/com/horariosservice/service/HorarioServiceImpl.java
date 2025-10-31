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
        // ⚙️ Si el rango abarca varias horas, seccionamos
        List<Horario> subHorarios = new ArrayList<>();

        LocalTime inicio = horario.getHoraInicio();
        LocalTime fin = horario.getHoraFin();
        LocalTime actual = inicio;

        while (actual.isBefore(fin)) {
            LocalTime siguiente = actual.plusHours(1);
            if (siguiente.isAfter(fin)) siguiente = fin;

            Horario bloque = new Horario();
            bloque.setDiaSemana(horario.getDiaSemana());
            bloque.setMedicoId(horario.getMedicoId());
            bloque.setHoraInicio(actual);
            bloque.setHoraFin(siguiente);
            bloque.setDisponible(true);
            bloque.setPacientesPorHora(horario.getPacientesPorHora());

            subHorarios.add(bloque);
            actual = siguiente;
        }

        horarioRepository.saveAll(subHorarios);
        // Retorna el primero como referencia
        return subHorarios.get(0);
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
}