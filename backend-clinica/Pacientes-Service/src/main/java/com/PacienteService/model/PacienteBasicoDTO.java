package com.PacienteService.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteBasicoDTO {
    private Long id;
    private String nombre;
    private String telefono;
}
