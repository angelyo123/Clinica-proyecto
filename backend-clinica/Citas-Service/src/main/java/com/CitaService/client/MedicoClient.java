package com.CitaService.client;


import com.CitaService.model.MedicoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "medicos-service", url = "http://localhost:8083/medico",
        configuration = com.CitaService.config.FeignConfig.class)
public interface MedicoClient {
    @GetMapping("/medico/{id}")
    Map<String, Object> findById(@PathVariable("id") Long id);

    @GetMapping("/obtener/{id}")
    MedicoDTO obtener(@PathVariable Long id);
}
