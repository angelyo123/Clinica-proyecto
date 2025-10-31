package com.AutomatizacionService.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequest {
    private Long idPaciente;
    private Long idMedico;
    private LocalDateTime fechaHora; // formato ISO-8601, ej. "2025-10-29T10:00"
}