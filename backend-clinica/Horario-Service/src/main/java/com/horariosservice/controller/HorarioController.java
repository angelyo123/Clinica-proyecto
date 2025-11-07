package com.horariosservice.controller;

import com.horariosservice.client.MedicoClient;
import com.horariosservice.model.Horario;
import com.horariosservice.repository.HorarioRepository;
import com.horariosservice.service.HorarioChangeTracker;
import com.horariosservice.service.HorarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/horarios")
@CrossOrigin(origins = "http://localhost:4200")
public class HorarioController {

    @Autowired
    private HorarioService horarioService;

    @Autowired
    private MedicoClient medicoClient;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired private HorarioChangeTracker tracker;
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

    @GetMapping("/public/cambios")
    @PreAuthorize("permitAll()")
    public Map<String, Object> verificarCambios(@RequestParam(required = false) String ultimaVersion) {
        LocalDateTime actual = tracker.obtenerUltimaActualizacion();
        boolean hayCambio = true;

        if (ultimaVersion != null && !ultimaVersion.isBlank()) {
            try {
                LocalDateTime ultima = LocalDateTime.parse(ultimaVersion);
                hayCambio = actual.isAfter(ultima);
            } catch (Exception e) {
                hayCambio = true;
            }
        }

        System.out.println("🕓 [Horarios] Última en tracker: " + actual +
                " | Última conocida: " + ultimaVersion +
                " | Cambio=" + hayCambio);

        return Map.of(
                "cambio", hayCambio,
                "ultimaVersion", actual.toString()
        );
    }

    @GetMapping("/todos")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<Horario>> listarTodos() {
        return ResponseEntity.ok(horarioService.listar());
    }


    @PutMapping("/{id}/disponibilidad/{estado}")
    public ResponseEntity<Void> actualizarDisponibilidad(@PathVariable Long id, @PathVariable boolean estado) {
        horarioService.actualizarDisponibilidad(id, estado);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Map<String, Object>> crear(@RequestBody Horario horario, Authentication auth) {
        String username = auth.getName();
        System.out.println("🔹 Usuario autenticado: " + username);

        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean esMedico = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO"));

        if (esMedico) {
            try {
                Map<String, Object> medico = medicoClient.obtenerPorUsuario(username);
                if (medico != null && medico.get("id") != null) {
                    Long medicoId = ((Number) medico.get("id")).longValue();
                    horario.setMedicoId(medicoId);
                    System.out.println("🩺 Asignando médico ID automáticamente: " + medicoId);
                } else {
                    throw new RuntimeException("Médico no encontrado para el usuario: " + username);
                }
            } catch (Exception e) {
                throw new RuntimeException("❌ Error al obtener médico desde MedicoService: " + e.getMessage());
            }
        } else if (esAdmin) {
            if (horario.getMedicoId() == null) {
                throw new RuntimeException("👑 El administrador debe indicar un médico válido (medicoId).");
            }
            System.out.println("👑 ADMIN creando horario con medicoId: " + horario.getMedicoId());
        }

        List<Horario> nuevos = horarioService.crear(horario);
        System.out.println("✅ Horarios creados correctamente: " + nuevos.size());
        tracker.marcarCambio();

        return ResponseEntity.ok(
                Map.of("mensaje", "✅ " + nuevos.size() + " bloques creados correctamente.", "data", nuevos)
        );
    }



    // ✅ Eliminar TODOS los horarios (solo ADMIN)
    @DeleteMapping("/eliminar/todos")
    public ResponseEntity<String> eliminarTodos() {
        horarioService.eliminarTodos();
        tracker.marcarCambio();
        return ResponseEntity.ok("🗑️ Todos los horarios han sido eliminados correctamente.");
    }


    // ✅ Actualizar horario existente
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Horario> actualizar(@PathVariable Long id, @RequestBody Horario horario, Authentication auth) {
        String username = auth.getName();
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Horario existente = horarioService.obtener(id);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        // Si es médico, validar que sea el dueño del horario
        if (!esAdmin) {
            try {
                Map<String, Object> medico = medicoClient.obtenerPorUsuario(username);
                if (medico == null || medico.get("id") == null) {
                    return ResponseEntity.status(403).build();
                }
                Long idMedico = ((Number) medico.get("id")).longValue();
                if (!idMedico.equals(existente.getMedicoId())) {
                    return ResponseEntity.status(403).build();
                }
            } catch (Exception e) {
                return ResponseEntity.status(403).build();
            }
        }

        Horario actualizado = horarioService.actualizar(id, horario);
        tracker.marcarCambio();
        return ResponseEntity.ok(actualizado);
    }

    // ✅ Eliminar horario
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Horario existente = horarioService.obtener(id);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        // Si es médico, validar que sea el dueño del horario
        if (!esAdmin) {
            try {
                Map<String, Object> medico = medicoClient.obtenerPorUsuario(username);
                if (medico == null || medico.get("id") == null) {
                    return ResponseEntity.status(403).build();
                }
                Long idMedico = ((Number) medico.get("id")).longValue();
                if (!idMedico.equals(existente.getMedicoId())) {
                    return ResponseEntity.status(403).build();
                }
            } catch (Exception e) {
                return ResponseEntity.status(403).build();
            }
        }

        horarioService.eliminar(id);
        tracker.marcarCambio();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/medico/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<Horario>> listarPorMedico(@PathVariable Long id) {
        return ResponseEntity.ok(horarioService.listarPorMedico(id));
    }

    @DeleteMapping("/eliminar/porMedico/{medicoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> eliminarPorMedico(@PathVariable Long medicoId) {
        horarioService.eliminarPorMedico(medicoId);
        tracker.marcarCambio();
        return ResponseEntity.ok("🗑️ Horarios del médico " + medicoId + " eliminados correctamente.");
    }

    @GetMapping("/mios")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<List<Horario>> listarMisHorarios(Authentication auth) {
        String username = auth.getName(); // el "sub" del token
        System.out.println("🧑‍⚕️ Médico logueado: " + username);

        Map<String, Object> medico = medicoClient.obtenerPorUsuario(username);
        System.out.println("🔹 Respuesta de medico-service: " + medico);

        if (medico == null || medico.get("id") == null) {
            throw new RuntimeException("No se encontró médico con usuario: " + username);
        }

        Long medicoId = ((Number) medico.get("id")).longValue();
        System.out.println("🔍 Buscando horarios del médico ID real: " + medicoId);

        List<Horario> horarios = horarioService.listarPorMedico(medicoId);
        return ResponseEntity.ok(horarios);
    }

}