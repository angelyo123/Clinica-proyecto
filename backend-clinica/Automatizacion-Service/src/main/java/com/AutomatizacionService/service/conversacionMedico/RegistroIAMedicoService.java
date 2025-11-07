package com.AutomatizacionService.service.conversacionMedico;

import com.AutomatizacionService.model.entity.medico.RegistroIAMedico;
import com.AutomatizacionService.repository.RegistroIAMedicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistroIAMedicoService {

    private final RegistroIAMedicoRepository repo;

    public RegistroIAMedicoService(RegistroIAMedicoRepository repo) {
        this.repo = repo;
    }

    /**
     * Guarda una nueva interacción entre el médico y la IA.
     */
    public void guardar(Long medicoId, String mensaje, String respuesta) {
        RegistroIAMedico registro = new RegistroIAMedico(medicoId, mensaje, respuesta);
        repo.save(registro);
        System.out.println("🧾 [Registro IA-Médico] Guardado: " + registro);
    }

    /**
     * Retorna el historial de conversaciones ordenado por fecha.
     */
    public List<RegistroIAMedico> listarPorMedico(Long medicoId) {
        return repo.findByMedicoIdOrderByFechaAsc(medicoId);
    }
}