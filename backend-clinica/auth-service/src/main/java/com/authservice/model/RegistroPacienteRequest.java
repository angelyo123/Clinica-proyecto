package com.authservice.model;


import lombok.Data;

@Data
public class RegistroPacienteRequest {


    private String nombre;
    private String dni;
    private String telefono;

    private String username;

    private String password;
}
