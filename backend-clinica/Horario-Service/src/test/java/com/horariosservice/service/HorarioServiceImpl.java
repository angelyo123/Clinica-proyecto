package com.horariosservice.service;

import com.horariosservice.model.Horario;
import com.horariosservice.repository.HorarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        return horarioRepository.save(horario);
    }

    @Override
    public Horario actualizar(Long id, Horario horario) {
        Horario existente = horarioRepository.findById(id).orElse(null);
        if (existente == null) return null;

        existente.setDiaSemana(horario.getDiaSemana());
        existente.setHoraInicio(horario.getHoraInicio());
        existente.setHoraFin(horario.getHoraFin());
        existente.setDisponible(horario.isDisponible());
        return horarioRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        horarioRepository.deleteById(id);
    }

    @Override
    public List<Horario> listarPorMedico(Long medicoId) {
        return horarioRepository.findByMedicoId(medicoId);
    }
}