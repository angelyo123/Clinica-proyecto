package com.AutomatizacionService.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatosCita {
    private String paciente;
    private String medico;
    private String pacienteId;
    private String medicoId;
}