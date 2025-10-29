package com.PacienteService.service;


import com.PacienteService.client.AuthClient;
import com.PacienteService.client.CitaClient;
import com.PacienteService.model.CitaDTO;
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

    @Autowired
    private CitaClient citaClient;

    @Override
    public List<Paciente> listar() {
        return pacienteRepository.findAll();
    }

    @Override
    public Paciente ObtenerId(Long id) {
        return pacienteRepository.findById(id).orElse(null);
    }

    @Override
    public Paciente Crear(Paciente paciente) {
        // 🔹 Paso 1: Crear usuario en el auth-service
        Map<String, Object> request = new HashMap<>();
        request.put("username", paciente.getDni());
        request.put("password", "1234");

        Map<String, Object> response = authClient.registrarUsuarioPaciente(request);

        // 🔹 Paso 2: Guardar paciente (guardamos solo username del usuario)
        paciente.setUsuario(response.get("username").toString());
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
        System.out.println("🗑️ Eliminando paciente ID: " + id);

        try {
            // 1️⃣ Eliminar todas las citas del paciente (una sola llamada)
            System.out.println("📋 Eliminando citas asociadas...");
            citaClient.eliminarPorPaciente(id);
            System.out.println("✅ Citas eliminadas");

            // 2️⃣ Eliminar el paciente
            System.out.println("📋 Eliminando paciente...");
            pacienteRepository.deleteById(id);
            System.out.println("✅ Paciente eliminado");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            throw new RuntimeException("Error al eliminar paciente", e);
        }
    }
    @Override
    public Paciente guardarPaciente(Paciente paciente) {
        return pacienteRepository.save(paciente);
    }

    public Paciente obtenerPorUsuario(String username) {
        return pacienteRepository.findByUsuario(username).orElse(null);
    }
}
