package com.MedicoService.service;

import com.MedicoService.client.AuthClient;
import com.MedicoService.model.Medico;
import com.MedicoService.repository.MedicoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MedicoServiceImpl implements MedicoService {

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private AuthClient authClient;

    @Override
    public List<Medico> listar() {
        return medicoRepository.findAll();
    }

    @Override
    public Medico obtenerPorId(Long id) {
        return medicoRepository.findById(id).orElse(null);
    }

    @Override
    public Medico crear(Medico medico) {
        Map<String, Object> request = new HashMap<>();
        request.put("username", medico.getDni());
        request.put("password", "1234");

        Map<String, Object> response = authClient.registrarUsuarioMedico(request);

        medico.setUsuario(response.get("username").toString());
        return medicoRepository.save(medico);
    }

    @Override
    public Medico actualizar(Long id, Medico medico) {
        Medico existente = medicoRepository.findById(id).orElse(null);
        if (existente != null) {
            existente.setNombre(medico.getNombre());
            existente.setEspecialidad(medico.getEspecialidad());
            existente.setTelefono(medico.getTelefono());
            existente.setDni(medico.getDni());
            return medicoRepository.save(existente);
        }
        return null;
    }

    @Override
    public void eliminar(Long id) {
        medicoRepository.deleteById(id);
    }

    public Medico obtenerPorUsuario(String username) {
        return medicoRepository.findByUsuario(username).orElse(null);
    }
}