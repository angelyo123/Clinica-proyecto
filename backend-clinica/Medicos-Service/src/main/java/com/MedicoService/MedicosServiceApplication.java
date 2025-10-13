package com.MedicoService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.MedicoService.client")
public class MedicosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicosServiceApplication.class, args);
    }

}
