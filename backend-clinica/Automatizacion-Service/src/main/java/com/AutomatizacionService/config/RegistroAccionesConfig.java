package com.AutomatizacionService.config;

import com.AutomatizacionService.service.LogicaHorario.ActualizacionHorariosService;
import com.AutomatizacionService.service.LogicaHorario.ListarHorariosService;
import com.AutomatizacionService.service.LogicaMedico.ListarMedicosService;
import com.AutomatizacionService.service.conversacionIA.AccionIARegistry;
import com.AutomatizacionService.service.conversacionIA.ActualizacionMedicosService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegistroAccionesConfig {

    @Autowired private ActualizacionMedicosService actualizacionMedicosService;
    @Autowired private ActualizacionHorariosService actualizacionHorariosService;

    private final AccionIARegistry registry;
    private final ListarMedicosService listarMedicosService;
    private final ListarHorariosService listarHorariosService;

    public RegistroAccionesConfig(
            AccionIARegistry registry,
            ListarMedicosService listarMedicosService,
            ListarHorariosService listarHorariosService) {
        this.registry = registry;
        this.listarMedicosService = listarMedicosService;
        this.listarHorariosService = listarHorariosService;
    }

    @PostConstruct
    public void registrarAcciones() {

        registry.registrarAccion(
                "listar_medicos",
                "Lista todos los médicos disponibles en la clínica",
                (pacienteId, parametros) -> {
                    actualizacionMedicosService.verificarYActualizar(pacienteId);
                    return listarMedicosService.listarMedicos(pacienteId, parametros);
                }
        );

        registry.registrarAccion(
                "listar_horarios",
                "Lista todos los horarios médicos disponibles en la clínica",
                (pacienteId, parametros) -> {
                    actualizacionHorariosService.verificarYActualizar(pacienteId);
                    return listarHorariosService.listarHorarios(pacienteId, parametros);
                }
        );

        System.out.println("✅ Acciones IA registradas: listar_medicos, listar_horarios");
    }
}