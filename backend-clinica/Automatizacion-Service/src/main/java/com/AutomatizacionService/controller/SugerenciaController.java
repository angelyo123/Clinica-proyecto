package com.AutomatizacionService.controller;

import com.AutomatizacionService.model.DatosCita;
import com.AutomatizacionService.service.SugerenciaIAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/automatizacion")
public class SugerenciaController {

    @Autowired
    private SugerenciaIAService sugerenciaIAService;

    // 🧠 Endpoint para mensajes de pacientes
    @PostMapping("/procesar")
    public ResponseEntity<Map<String, Object>> procesar(@RequestBody Map<String, Object> solicitud) {
        return ResponseEntity.ok(sugerenciaIAService.procesarMensajeNatural(solicitud));
    }

    // 👨‍⚕️ Endpoint para mensajes de médicos
    @PostMapping("/medico")
    public ResponseEntity<Map<String, Object>> procesarMedico(@RequestBody Map<String, Object> solicitud) {
        return ResponseEntity.ok(sugerenciaIAService.procesarMensajeMedico(solicitud));
    }
}