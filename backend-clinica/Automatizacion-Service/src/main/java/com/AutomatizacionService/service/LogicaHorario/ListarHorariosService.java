package com.AutomatizacionService.service.LogicaHorario;

import com.AutomatizacionService.client.HorarioClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ListarHorariosService {

    private final HorarioClient horarioClient;
    private String ultimaVersionCacheada = null;
    private List<Map<String, Object>> ultimoListado = null;

    public ListarHorariosService(HorarioClient horarioClient) {
        this.horarioClient = horarioClient;
    }

    public Map<String, Object> listarHorarios(Long pacienteId, Map<String, Object> params) {
        System.out.println("📡 Verificando si hubo cambios en horarios...");

        Map<String, Object> versionData = horarioClient.verificarCambios(ultimaVersionCacheada);
        boolean hayCambios = Boolean.TRUE.equals(versionData.get("cambio"));

        if (hayCambios || ultimoListado == null) {
            System.out.println("🔄 Detectado cambio en lista de horarios → actualizando...");
            ultimoListado = horarioClient.listarTodos();
            ultimaVersionCacheada = (String) versionData.get("ultimaVersion");
        } else {
            System.out.println("✅ No hay cambios → usando cache local.");
        }

        if (ultimoListado == null || ultimoListado.isEmpty()) {
            return Map.of("mensaje", "No hay horarios registrados en este momento.");
        }

        return Map.of(
                "mensaje", "🕒 Horarios actualizados obtenidos correctamente.",
                "data", ultimoListado
        );
    }
}