package com.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPacienteRequest {
    private String nombre;
    private String dni;
    private String telefono;
    private String username;
    private String password;
}