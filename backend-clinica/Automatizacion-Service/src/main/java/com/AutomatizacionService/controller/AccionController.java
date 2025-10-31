package com.AutomatizacionService.controller;

import com.AutomatizacionService.service.conversacionIA.AccionIARegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccionController {

    private final AccionIARegistry accionRegistry;

    public AccionController(AccionIARegistry accionRegistry) {
        this.accionRegistry = accionRegistry;
    }

    @GetMapping("/acciones")
    public Object listarAcciones() {
        return accionRegistry.getAcciones().values();
    }
}