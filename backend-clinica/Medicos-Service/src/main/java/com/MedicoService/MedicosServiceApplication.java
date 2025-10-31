package com.MedicoService;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.MedicoService.client")
public class MedicosServiceApplication {

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        SpringApplication.run(MedicosServiceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        System.out.println("✅ JVM usando encoding: " + System.getProperty("file.encoding"));
    }

}
