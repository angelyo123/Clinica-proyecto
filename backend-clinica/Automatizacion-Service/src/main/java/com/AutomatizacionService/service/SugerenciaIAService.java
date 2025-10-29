package com.AutomatizacionService.service;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.client.HorarioClient;
import com.AutomatizacionService.client.MedicoClient;
import com.AutomatizacionService.client.PacienteClient;
import com.AutomatizacionService.model.DatosCita;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SugerenciaIAService {

    private final DeepSeekService deepSeekService;
    private final PacienteClient pacienteClient;
    private final MedicoClient medicoClient;
    private final HorarioClient horarioClient;
    private final CitaClient citaClient;

    public SugerenciaIAService(
            DeepSeekService deepSeekService,
            PacienteClient pacienteClient,
            MedicoClient medicoClient,
            HorarioClient horarioClient,
            CitaClient citaClient) {
        this.deepSeekService = deepSeekService;
        this.pacienteClient = pacienteClient;
        this.medicoClient = medicoClient;
        this.horarioClient = horarioClient;
        this.citaClient = citaClient;
    }

    public Map<String, Object> procesarMensajeNatural(Map<String, Object> solicitud) {
        String mensaje = solicitud.get("mensaje").toString();
        Long pacienteId = Long.valueOf(solicitud.get("pacienteId").toString());

        Map<String, Object> paciente = pacienteClient.obtenerPacientePublico(pacienteId);

        // 🧠 Paso 1: Analizar con IA el mensaje
        String analisis = deepSeekService.generarTexto(
                "Analiza el siguiente mensaje de un paciente para una cita médica: " + mensaje +
                        ". Devuelve la especialidad médica requerida y una fecha/hora si se menciona."
        );

        // 🔍 Paso 2: extraer especialidad del texto IA (ejemplo simplificado)
        String especialidad = analisis.contains("cardi") ? "Cardiología" :
                analisis.contains("dermat") ? "Dermatología" :
                        "Medicina General";

        // 🔎 Paso 3: buscar médicos disponibles
        List<Map<String, Object>> medicos = medicoClient.listarPorEspecialidad(especialidad);
        if (medicos.isEmpty()) {
            return Map.of("mensaje", "No hay médicos disponibles en la especialidad " + especialidad);
        }

        Map<String, Object> medico = medicos.get(0);
        Long medicoId = Long.valueOf(medico.get("id").toString());
        List<Map<String, Object>> horarios = horarioClient.listarPorMedico(medicoId);

        if (horarios.isEmpty()) {
            String sugerencia = deepSeekService.generarTexto(
                    "No hay disponibilidad para el mensaje '" + mensaje + "'. " +
                            "Sugiérele al paciente un horario alternativo y explica de forma amable."
            );
            return Map.of("mensaje", sugerencia);
        }

        // ✅ Paso 4: crear cita con el primer horario disponible
        Map<String, Object> horario = horarios.get(0);
        Map<String, Object> citaData = Map.of(
                "pacienteId", pacienteId,
                "medicoId", medicoId,
                "descripcion", mensaje,
                "fecha", horario.get("fecha"),
                "hora", horario.get("hora")
        );
        citaClient.crearCita(citaData);

        return Map.of(
                "mensaje", String.format("Se ha agendado tu cita con %s (%s) el %s a las %s.",
                        medico.get("nombre"), especialidad,
                        horario.get("fecha"), horario.get("hora")),
                "especialidad", especialidad,
                "medico", medico,
                "paciente", paciente
        );
    }
}