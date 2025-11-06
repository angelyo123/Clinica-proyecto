package com.horariosservice.service;


import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HorarioChangeTracker {

    private LocalDateTime ultimaActualizacion = LocalDateTime.now();

    public void marcarCambio() {
        ultimaActualizacion = LocalDateTime.now();
        System.out.println("📢 [Tracker Horarios] Cambio registrado → " + ultimaActualizacion);
    }

    public LocalDateTime obtenerUltimaActualizacion() {
        return ultimaActualizacion;
    }
}