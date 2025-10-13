package com.PacienteService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;

@SpringBootApplication
@FeignClient
@EnableFeignClients(basePackages = "com.PacienteService.client")
public class PacientesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PacientesServiceApplication.class, args);
    }

}
