package com.clinica.backendclinica.service;

import com.clinica.backendclinica.model.Cita;
import com.clinica.backendclinica.model.Medico;
import com.clinica.backendclinica.model.Paciente;
import com.clinica.backendclinica.model.Usuario;
import com.clinica.backendclinica.repository.CitaRepository;
import com.clinica.backendclinica.repository.MedicoRepository;
import com.clinica.backendclinica.repository.PacienteRepository;
import com.clinica.backendclinica.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CitaServiceImpl implements CitaService {

    @Autowired
    CitaRepository citaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;


    @Override
    public Cita crearCita(Cita cita) {

        boolean existe = citaRepository.existsByPacienteAndFechaHora(cita.getPaciente(), cita.getFechaHora());
        if(existe){
            throw new RuntimeException("Cita ya existe");
        }

        return citaRepository.save(cita);
    }

    @Override
    public Cita actualizarCita(Cita cita, Long id) {
        Cita existe = citaRepository.findById(id).orElse(null);
        if(existe!=null){

            existe.setEstado(cita.getEstado());
            existe.setMedico(cita.getMedico());
            existe.setPaciente(cita.getPaciente());

            return  citaRepository.save(existe);
        }
        else{
            return null;
        }
    }

    @Override
    public void eliminarCita(Long id) {

        citaRepository.deleteById(id);
    }


    // 🔹 Método para listar dinámicamente según autenticación
    @Override
    public List<Cita> listarCitas(Authentication auth) {
        Usuario usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean esAdmin = usuario.getRoles().stream()
                .anyMatch(r -> r.getNombre().equals("ROLE_ADMIN"));
        boolean esMedico = usuario.getRoles().stream()
                .anyMatch(r -> r.getNombre().equals("ROLE_MEDICO"));

        if (esAdmin) return citaRepository.findAll();

        if (esMedico) {
            Medico medico = medicoRepository.findByUsuario(usuario)
                    .orElseThrow(() -> new RuntimeException("Médico no encontrado"));
            return citaRepository.findByMedico(medico);
        }

        // Paciente
        Paciente paciente = pacienteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        return citaRepository.findByPaciente(paciente);
    }

    // 🔹 Nuevo: listar citas por paciente ID
    @Override
    public List<Cita> listarPorPaciente(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        return citaRepository.findByPaciente(paciente);
    }

    // 🔹 Nuevo: listar citas por médico ID
    @Override
    public List<Cita> listarPorMedico(Long medicoId) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));
        return citaRepository.findByMedico(medico);
    }

    // 🔹 Nuevo: listar todas las citas (para admin)
    @Override
    public List<Cita> listarTodas() {
        return citaRepository.findAll();
    }

    @Override
    public Cita buscarCita(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
    }
}
