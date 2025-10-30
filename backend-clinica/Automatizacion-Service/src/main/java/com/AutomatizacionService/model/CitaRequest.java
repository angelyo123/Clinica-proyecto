package com.AutomatizacionService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequest {
    private Long idPaciente;
    private Long idMedico;
    private String fechaHora; // formato ISO-8601, ej. "2025-10-29T10:00"
}