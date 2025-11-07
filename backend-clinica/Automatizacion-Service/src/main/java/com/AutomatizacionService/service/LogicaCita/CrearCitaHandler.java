package com.AutomatizacionService.service.LogicaCita;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.client.PacienteClient;
import com.AutomatizacionService.model.dto.CitaRequest;
import com.AutomatizacionService.service.LogicaMedico.ListarMedicosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CrearCitaHandler {

    @Autowired
    private CitaClient citaClient;
    @Autowired private PacienteClient pacienteClient;
    @Autowired private ListarMedicosService listarMedicosService;

    public Map<String, Object> crearCita(Long pacienteId, Map<String, Object> parametros) {
        System.out.println("🩺 [Handler] Ejecutando acción: crear_cita");

        // 1️⃣ Obtener datos del paciente
        Map<String, Object> paciente = pacienteClient.obtenerPacientePublico(pacienteId);
        if (paciente == null || paciente.get("id") == null) {
            return Map.of(
                    "success", false,
                    "error", "No se pudo obtener la información del paciente."
            );
        }

        // 2️⃣ Validar parámetros
        if (parametros == null) {
            return Map.of(
                    "success", false,
                    "error", "No se enviaron parámetros de cita."
            );
        }

        Long medicoId = ((Number) parametros.getOrDefault("medicoId", 0)).longValue();
        Long horarioId = ((Number) parametros.getOrDefault("idHorario", 0)).longValue();
        String fechaHora = (String) parametros.getOrDefault("fechaHora", null);

        if (medicoId == 0 || horarioId == 0 || fechaHora == null) {
            return Map.of(
                    "success", false,
                    "error", "Faltan datos para crear la cita (medicoId, idHorario o fechaHora)."
            );
        }

        // 3️⃣ Construir el request DTO
        CitaRequest request = new CitaRequest();
        request.setFechaCita(LocalDateTime.parse(fechaHora));  // ✅ antes setFechaHora
        request.setMedico(Map.of("id", medicoId));
        request.setPaciente(Map.of("id", pacienteId));
        request.setIdHorario(horarioId);

        System.out.println("📡 Enviando a cita-service: " + request);

        // 4️⃣ Enviar al microservicio de Cita
        Map<String, Object> respuesta;
        try {
            respuesta = citaClient.crearCita(request);
            System.out.println("📥 Respuesta cita-service: " + respuesta);
        } catch (Exception e) {
            System.err.println("❌ Error al invocar cita-service: " + e.getMessage());
            e.printStackTrace();
            return Map.of(
                    "success", false,
                    "error", "No se pudo crear la cita en el microservicio de citas.",
                    "detalle", e.getMessage()
            );
        }

        // 5️⃣ Validar estructura y contenido de la respuesta
        if (respuesta == null) {
            return Map.of(
                    "success", false,
                    "error", "No se recibió respuesta del microservicio de citas."
            );
        }

        Object dataObj = respuesta.containsKey("data") ? respuesta.get("data") : respuesta;
        if (!(dataObj instanceof Map<?, ?> dataMap)) {
            return Map.of(
                    "success", false,
                    "error", "Formato inesperado en la respuesta del microservicio de citas.",
                    "data", respuesta
            );
        }

        if (dataMap.get("id") == null) {
            return Map.of(
                    "success", false,
                    "error", "El microservicio no devolvió una cita válida. Puede que el horario esté ocupado o ya exista una cita en esa fecha.",
                    "data", respuesta
            );
        }

        // 6️⃣ Buscar nombre del médico (opcional para mensaje bonito)
        List<Map<String, Object>> medicos = (List<Map<String, Object>>)
                listarMedicosService.listarMedicos(pacienteId, Map.of()).get("data");

        String nombreMedico = medicos.stream()
                .filter(m -> ((Number) m.get("id")).longValue() == medicoId)
                .map(m -> (String) m.get("nombre"))
                .findFirst()
                .orElse("el médico seleccionado");

        // 7️⃣ Crear mensaje humano
        String nombrePaciente = String.valueOf(paciente.getOrDefault("nombre", "Paciente"));
        String mensaje = String.format(
                "✅ Cita creada correctamente para %s con el Dr. %s el %s. ¡Nos vemos pronto! 💙",
                nombrePaciente, nombreMedico, fechaHora
        );

        // 8️⃣ Devolver resultado consolidado
        return Map.of(
                "success", true,
                "mensaje", mensaje,
                "data", dataMap
        );
    }
}