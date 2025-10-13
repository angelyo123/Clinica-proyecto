package com.horariosservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalTime;

@Entity
@Data
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long medicoId;
    private String diaSemana; // LUNES, MARTES, etc.
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean disponible = true;
}
