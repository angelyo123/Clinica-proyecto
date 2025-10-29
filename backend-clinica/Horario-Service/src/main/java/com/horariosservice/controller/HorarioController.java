package com.horariosservice.controller;

import com.horariosservice.model.Horario;
import com.horariosservice.service.HorarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/horarios")
@CrossOrigin(origins = "http://localhost:4200")
public class HorarioController {

    @Autowired
    private HorarioService horarioService;

    // ✅ Listar todos los horarios
    @GetMapping
    //@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO')")
    public ResponseEntity<List<Horario>> listar() {
        return ResponseEntity.ok(horarioService.listar());
    }

    // ✅ Obtener un horario por ID
    @GetMapping("/{id}")
    //@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO')")
    public ResponseEntity<Horario> obtener(@PathVariable Long id) {
        Horario horario = horarioService.obtener(id);
        if (horario == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(horario);
    }

    // ✅ Crear un nuevo horario (solo médicos o admin)
    @PostMapping
    //@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO')")
    public ResponseEntity<Horario> crear(@RequestBody Horario horario) {
        Horario nuevo = horarioService.crear(horario);
        return ResponseEntity.ok(nuevo);
    }

    // ✅ Actualizar horario existente
    @PutMapping("/{id}")
    //@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO')")
    public ResponseEntity<Horario> actualizar(@PathVariable Long id, @RequestBody Horario horario) {
        Horario actualizado = horarioService.actualizar(id, horario);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(actualizado);
    }

    // ✅ Eliminar horario
    @DeleteMapping("/{id}")
    //@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        horarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/medico/{id}")
    //@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO')")
    public ResponseEntity<List<Horario>> listarPorMedico(@PathVariable Long id) {
        return ResponseEntity.ok(horarioService.listarPorMedico(id));
    }

}