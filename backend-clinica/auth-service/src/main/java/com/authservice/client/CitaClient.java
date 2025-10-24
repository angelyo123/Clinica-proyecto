package com.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "cita-service", url = "http://localhost:8084" )
public interface CitaClient {
    @GetMapping("/cita/listar")
    List<Object> listarCitas();
}