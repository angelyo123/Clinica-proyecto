package com.AutomatizacionService.controller.conversacionMedico;

import com.AutomatizacionService.service.conversacionMedico.ConversacionMedicoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ia/medico")
public class ConversacionMedicoController {

    private final ConversacionMedicoService conversacionMedicoService;

    public ConversacionMedicoController(ConversacionMedicoService conversacionMedicoService) {
        this.conversacionMedicoService = conversacionMedicoService;
    }

    @PostMapping("/conversar")
    public Map<String, Object> conversar(@RequestBody Map<String, Object> solicitud) {
        return conversacionMedicoService.procesarConversacion(solicitud);
    }
}