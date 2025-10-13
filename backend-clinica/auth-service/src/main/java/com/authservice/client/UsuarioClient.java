package com.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "auth-service", url = "http://localhost:8081")
public interface UsuarioClient {
    @GetMapping("/usuarios")
    List<Object> listarUsuarios();
}
