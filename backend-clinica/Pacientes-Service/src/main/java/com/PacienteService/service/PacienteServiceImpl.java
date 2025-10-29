package com.PacienteService.service;


import com.PacienteService.client.AuthClient;
import com.PacienteService.model.Paciente;
import com.PacienteService.repository.PacienteRepository;
import com.PacienteService.service.PacienteService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PacienteServiceImpl implements PacienteService {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private AuthClient authClient;

    @Override
    public List<Paciente> listar() {
        return pacienteRepository.findAll();
    }

    @Override
    public Paciente ObtenerId(Long id) {
        return pacienteRepository.findById(id).orElse(null);
    }

    public Paciente Crear(Paciente paciente) {


        if (paciente.getUsuario() == null) {

            paciente.setUsuario(paciente.getDni());
        }

        return pacienteRepository.save(paciente);
    }

    @Override
    public Paciente Actualizar(Long id, Paciente paciente) {
        Paciente existente = pacienteRepository.findById(id).orElse(null);
        if (existente != null) {
            existente.setNombre(paciente.getNombre());
            existente.setTelefono(paciente.getTelefono());
            existente.setDni(paciente.getDni());
            return pacienteRepository.save(existente);
        }
        return null;
    }

    @Override
    public void Eliminar(Long id) {
        pacienteRepository.deleteById(id);
    }

    @Override
    public Paciente guardarPaciente(Paciente paciente) {

        return pacienteRepository.save(paciente);
    }

    public Paciente obtenerPorUsuario(String username) {
        return pacienteRepository.findByUsuario(username).orElse(null);
    }
}
