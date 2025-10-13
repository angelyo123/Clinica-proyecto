package com.clinica.backendclinica.service;

import com.clinica.backendclinica.model.Usuario;

import java.util.List;

public interface UsuarioService {
    List<Usuario> BuscarTodos();
    Usuario BuscarPorId(Long id);
    Usuario Crear(Usuario usuario);
    Usuario Actualizar(Long id, Usuario usuario);
    void Eliminar(Long id);
    Usuario asignarRol(Usuario usuario, String nombreRol);

}
