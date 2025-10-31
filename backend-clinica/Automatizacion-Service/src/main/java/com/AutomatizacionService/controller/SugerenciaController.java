package com.AutomatizacionService.controller;

import com.AutomatizacionService.service.orquestador.SugerenciaIAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/automatizacion/procesar")
public class SugerenciaController {

    @Autowired
    private SugerenciaIAService sugerenciaIAService;

    /**
     * 🧠 Procesa mensaje de un PACIENTE (IA analiza síntomas y crea cita).
     * JSON esperado:
     * {
     *   "pacienteId": 2,
     *   "mensaje": "tengo un dolor de cabeza desde hace 3 días"
     * }
     */
    @PostMapping("/paciente")
    public ResponseEntity<Map<String, Object>> procesarPaciente(@RequestBody Map<String, Object> solicitud) {
        return ResponseEntity.ok(sugerenciaIAService.procesarPaciente(solicitud));
    }

    /**
     * 👨‍⚕️ Procesa mensaje de un MÉDICO (IA registra horarios).
     * JSON esperado:
     * {
     *   "medicoId": 3,
     *   "mensaje": "estaré disponible mañana de 9am a 12pm"
     * }
     */
    @PostMapping("/medico")
    public ResponseEntity<Map<String, Object>> procesarMedico(@RequestBody Map<String, Object> solicitud) {
        return ResponseEntity.ok(sugerenciaIAService.procesarMedico(solicitud));
    }

    /**
     * ✅ Procesa CONFIRMACIÓN de cita del paciente.
     * JSON esperado:
     * {
     *   "pacienteId": 2,
     *   "mensaje": "sí, confirmo mi cita con el dermatólogo"
     * }
     */
    @PostMapping("/confirmacion")
    public ResponseEntity<Map<String, Object>> procesarConfirmacion(@RequestBody Map<String, Object> solicitud) {
        return ResponseEntity.ok(sugerenciaIAService.procesarConfirmacion(solicitud));
    }

    /**
     * ❌ Procesa CANCELACIÓN de cita (por paciente o IA).
     * JSON esperado:
     * {
     *   "pacienteId": 2,
     *   "mensaje": "no podré asistir a mi cita de mañana"
     * }
     */
    @PostMapping("/cancelacion")
    public ResponseEntity<Map<String, Object>> procesarCancelacion(@RequestBody Map<String, Object> solicitud) {
        return ResponseEntity.ok(sugerenciaIAService.procesarCancelacion(solicitud));
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("estado", "OK", "mensaje", "Automatización activa 🚀"));
    }

    @PostMapping("/conversacion")
    public ResponseEntity<Map<String, Object>> procesarConversacion(@RequestBody Map<String, Object> solicitud) {
        return ResponseEntity.ok(sugerenciaIAService.procesarConversacion(solicitud));
    }

}