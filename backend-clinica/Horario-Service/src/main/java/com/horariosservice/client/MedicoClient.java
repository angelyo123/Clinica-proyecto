package com.horariosservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "medicos-service", url = "http://localhost:8083/medico")
public interface MedicoClient {

    @GetMapping("/usuario/{username}")
    Map<String, Object> obtenerPorUsuario(@PathVariable("username") String username);

    @GetMapping("/obtener/{id}")
    Map<String, Object> obtener(@PathVariable("id") Long id);
}