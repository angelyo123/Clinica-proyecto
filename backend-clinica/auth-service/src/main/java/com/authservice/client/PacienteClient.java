package com.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "paciente-service", url = "http://localhost:8082")
public interface PacienteClient {
    @GetMapping("/paciente/listar")
    List<Object> listarPacientes();
}