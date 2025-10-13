package com.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "medico-service", url = "http://localhost:8083")
public interface MedicoClient {
    @GetMapping("/medico/listar")
    List<Object> listarMedicos();
}