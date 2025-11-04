package com.AutomatizacionService.controller;

import com.AutomatizacionService.service.LogicaMedico.ListarMedicosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/accion/medicos")
public class ListarMedicosController {

    private final ListarMedicosService listarMedicosService;

    public ListarMedicosController(ListarMedicosService listarMedicosService) {
        this.listarMedicosService = listarMedicosService;
    }

    @GetMapping("/listar")
    public Map<String, Object> listar() {
        // 🔧 pacienteId puede ser fijo solo para test
        return listarMedicosService.listarMedicos(1L, Map.of());
    }
}