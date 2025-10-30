package com.horariosservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.horariosservice.client")
public class HorarioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HorarioServiceApplication.class, args);
    }

}
