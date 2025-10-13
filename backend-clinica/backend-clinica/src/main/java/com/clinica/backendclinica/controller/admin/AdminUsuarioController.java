package com.clinica.backendclinica.controller.admin;

import com.clinica.backendclinica.model.Usuario;
import com.clinica.backendclinica.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/usuario")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/listar")
    public List<Usuario> listar() {
        return usuarioService.BuscarTodos();
    }

    @GetMapping("/obtener/{id}")
    public Usuario obtener(@PathVariable Long id) {
        return usuarioService.BuscarPorId(id);
    }

    @DeleteMapping("/eliminar/{id}")
    public void eliminar(@PathVariable Long id) {
        usuarioService.Eliminar(id);
    }
}
