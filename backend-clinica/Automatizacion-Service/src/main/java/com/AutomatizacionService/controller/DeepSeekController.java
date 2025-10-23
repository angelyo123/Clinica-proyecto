package com.AutomatizacionService.controller;

import com.AutomatizacionService.service.DeepSeekService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public class DeepSeekController {

    private final DeepSeekService deepSeekService;

    public DeepSeekController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/sugerir")
    public ResponseEntity<String> sugerir(@RequestBody Map<String, String> req) {
        String prompt = req.get("prompt");
        String respuesta = deepSeekService.generarTexto(prompt);
        return ResponseEntity.ok(respuesta);
    }
}
