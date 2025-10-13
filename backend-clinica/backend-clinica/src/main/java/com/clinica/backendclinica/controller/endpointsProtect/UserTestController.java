package com.clinica.backendclinica.controller.endpointsProtect;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserTestController {

    @GetMapping("/hello")
    public String helloUser() {
        return "Hola, este endpoint es accesible por USER o ADMIN";
    }
}

