 package com.CitaService.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class CitaMedicoDTO {
    private Long id;
    private LocalDateTime fechaHora;
    private String estado;

    private PacienteDTO paciente;
    private Map<String, Object> medico;
}