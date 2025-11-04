package com.AutomatizacionService.service.LogicaMedico;

import com.AutomatizacionService.client.MedicoClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListarMedicosService {

    private final MedicoClient medicoClient;
    private String ultimaVersionCacheada = null;
    private List<Map<String, Object>> ultimoListado = null;

    public ListarMedicosService(MedicoClient medicoClient) {
        this.medicoClient = medicoClient;
    }

    public Map<String, Object> listarMedicos(Long pacienteId, Map<String, Object> params) {
        System.out.println("📡 Verificando si hubo cambios en médicos...");

        Map<String, Object> versionData = medicoClient.verificarCambios(ultimaVersionCacheada);
        boolean hayCambios = Boolean.TRUE.equals(versionData.get("cambio"));

        if (hayCambios || ultimoListado == null) {
            System.out.println("🔄 Detectado cambio en lista de médicos → actualizando...");
            ultimoListado = medicoClient.listarMedicos();
            ultimaVersionCacheada = (String) versionData.get("ultimaVersion");
        } else {
            System.out.println("✅ No hay cambios → usando cache local.");
        }

        if (ultimoListado == null || ultimoListado.isEmpty()) {
            return Map.of("mensaje", "No hay médicos registrados en este momento.");
        }

        String texto = ultimoListado.stream()
                .map(m -> "- " + m.get("nombre") + " (" + m.get("especialidad") + ")")
                .collect(Collectors.joining("\n"));

        return Map.of(
                "mensaje", "👨‍⚕️ Médicos disponibles:\n" + texto,
                "data", ultimoListado
        );
    }
}