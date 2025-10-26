package com.authservice.controller.protect.endpointsProtect;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequestMapping("/admin")
public class AdminTestController {

    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "Este endpoint es solo para ADMIN";
    }
}