package com.clinica.backendclinica.controller.endpointsProtect;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminTestController {

    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "Este endpoint es solo para ADMIN";
    }
}