package com.AutomatizacionService.service.conversacionIA;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Component
public class AccionIARegistry {

    private final Map<String, AccionIA> acciones = new HashMap<>();

    public void registrarAccion(String nombre, String descripcion,
                                BiFunction<Long, Map<String, Object>, Map<String, Object>> metodo) {
        acciones.put(nombre, new AccionIA(nombre, descripcion, metodo));
    }

    public Map<String, AccionIA> getAcciones() {
        return acciones;
    }

    public record AccionIA(
            String nombre,
            String descripcion,
            BiFunction<Long, Map<String, Object>, Map<String, Object>> metodo
    ) {}
}
