package com.CitaService.model;

import lombok.Data;

@Data
public class MedicoDTO {
    private Long id;
    private String nombre;
    private String especialidad;
    private String telefono;
    private String dni;
    private String usuario;
}