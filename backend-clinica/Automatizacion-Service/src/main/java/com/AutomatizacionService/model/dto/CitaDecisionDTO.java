package com.AutomatizacionService.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CitaDecisionDTO {

    private Long pacienteId;
    private Long medicoId;
    private String especialidad;
    private String fecha;
    private String hora;
    private String mensaje;
}
