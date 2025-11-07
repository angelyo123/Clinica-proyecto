package com.AutomatizacionService.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequest {
    private LocalDateTime fechaCita;  // fecha de la cita real
    private Map<String, Object> medico;
    private Map<String, Object> paciente;
    private Long idHorario;
}