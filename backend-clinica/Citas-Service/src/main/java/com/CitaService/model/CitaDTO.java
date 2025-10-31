package com.CitaService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaDTO {
    private Long id;
    private LocalDateTime fechaHora;
    private String estado;
    private MedicoDTO medico;
    private PacienteDTO paciente;
}