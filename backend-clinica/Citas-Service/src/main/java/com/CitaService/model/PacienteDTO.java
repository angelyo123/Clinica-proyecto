package com.CitaService.model;

import lombok.Data;

@Data
public class PacienteDTO {

    private Long id;
    private String nombre;
    private String telefono;
    private String dni;
    private String usuario;
}
