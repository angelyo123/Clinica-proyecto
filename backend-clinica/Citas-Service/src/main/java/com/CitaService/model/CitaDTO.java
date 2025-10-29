package com.CitaService.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class CitaDTO {
    private Long id;
    private LocalDateTime fechaHora;
    private String estado;
    private MedicoDTO medico;
    private PacienteDTO paciente;
}