package com.AutomatizacionService.controller;

import com.AutomatizacionService.service.conversacionIA.ConversacionPacienteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ia")
public class ConversacionController {

    private final ConversacionPacienteService conversacionService;

    public ConversacionController(ConversacionPacienteService conversacionService) {
        this.conversacionService = conversacionService;
    }

    @PostMapping("/conversar")
    public Map<String, Object> conversar(@RequestBody Map<String, Object> solicitud) {
        return conversacionService.procesarConversacion(solicitud);
    }
}