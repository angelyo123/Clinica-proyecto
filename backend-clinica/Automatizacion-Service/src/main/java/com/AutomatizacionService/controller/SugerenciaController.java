package com.AutomatizacionService.controller;

import com.AutomatizacionService.model.DatosCita;
import com.AutomatizacionService.service.SugerenciaIAService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ia")
public class SugerenciaController {

    private final SugerenciaIAService sugerenciaIAService;

    public SugerenciaController(SugerenciaIAService sugerenciaIAService) {
        this.sugerenciaIAService = sugerenciaIAService;
    }

    @PostMapping("/procesar-mensaje")
    public ResponseEntity<Map<String, Object>> procesar(@RequestBody Map<String, Object> solicitud) {
        Map<String, Object> resultado = sugerenciaIAService.procesarMensajeNatural(solicitud);
        return ResponseEntity.ok(resultado);
    }
}
