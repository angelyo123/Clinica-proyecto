package com.MedicoService.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MedicoChangeTracker {

    private LocalDateTime ultimaActualizacion = LocalDateTime.now();

    public void marcarCambio() {
        ultimaActualizacion = LocalDateTime.now();
        System.out.println("📢 [Tracker] Cambio registrado → " + ultimaActualizacion);
    }

    public LocalDateTime obtenerUltimaActualizacion() {
        return ultimaActualizacion;
    }
}