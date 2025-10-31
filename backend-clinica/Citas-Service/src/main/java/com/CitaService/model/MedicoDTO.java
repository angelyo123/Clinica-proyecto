package com.CitaService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicoDTO {
    private Long id;
    private String nombre;
    private String especialidad;
    private String telefono;
    private String dni;
    private String usuario;
}