package com.AutomatizacionService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.AutomatizacionService.client")
public class AutomatizacionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutomatizacionServiceApplication.class, args);
    }

}
