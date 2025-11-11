package com.authservice.client;

import com.authservice.model.PacienteDatosDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "paciente-service", url = "http://localhost:8082")
public interface PacienteClient {
    @GetMapping("/paciente/listar")
    List<Object> listarPacientes();

    @PostMapping("/paciente/crear")
    PacienteDatosDTO crearPaciente(@RequestBody PacienteDatosDTO datosPaciente);

    @GetMapping("/paciente/public/obtenerPorUsername/{username}")
    PacienteDatosDTO obtenerPorUsername(@PathVariable("username") String username);

}