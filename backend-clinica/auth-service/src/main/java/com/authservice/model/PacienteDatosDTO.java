package com.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDatosDTO {

    private Long id;
    private String nombre;
    private String dni;
    private String telefono;
    private String usuario;
}