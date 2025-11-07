package com.AutomatizacionService.config;

import com.AutomatizacionService.client.CitaClient;
import com.AutomatizacionService.client.PacienteClient;
import com.AutomatizacionService.model.dto.CitaRequest;
import com.AutomatizacionService.service.LogicaCita.CrearCitaHandler;
import com.AutomatizacionService.service.LogicaHorario.ActualizacionHorariosService;
import com.AutomatizacionService.service.LogicaHorario.ListarHorariosService;
import com.AutomatizacionService.service.LogicaMedico.ListarMedicosService;
import com.AutomatizacionService.service.conversacionIA.AccionIARegistry;
import com.AutomatizacionService.service.conversacionIA.ActualizacionMedicosService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class RegistroAccionesConfig {

    @Autowired private ActualizacionMedicosService actualizacionMedicosService;
    @Autowired private ActualizacionHorariosService actualizacionHorariosService;

    @Autowired private PacienteClient pacienteClient;
    @Autowired private CitaClient citaClient;

    private final AccionIARegistry registry;
    private final ListarMedicosService listarMedicosService;
    private final ListarHorariosService listarHorariosService;
    @Autowired private CrearCitaHandler crearCitaHandler;

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
                "listar_medicos_y_horarios",
                "Lista todos los médicos junto con sus horarios disponibles",
                (pacienteId, parametros) -> {
                    System.out.println("📦 Ejecutando acción combinada: listar_medicos_y_horarios");

                    // Primero aseguramos que ambos conjuntos estén actualizados
                    actualizacionMedicosService.verificarYActualizar(pacienteId);
                    actualizacionHorariosService.verificarYActualizar(pacienteId);

                    // Luego obtenemos los datos actualizados de cada servicio
                    Map<String, Object> medicos = listarMedicosService.listarMedicos(pacienteId, parametros);
                    Map<String, Object> horarios = listarHorariosService.listarHorarios(pacienteId, parametros);

                    // Fusionamos en una sola estructura JSON
                    Map<String, Object> dataFusionada = Map.of(
                            "medicos", medicos.get("data"),
                            "horarios", horarios.get("data")
                    );

                    return Map.of(
                            "mensaje", "✅ Médicos y horarios actualizados combinados correctamente.",
                            "data", dataFusionada
                    );
                }
        );

        // 🔹 Nueva acción: crear cita automáticamente
        // 🔹 Acción modular: crear cita automática
        registry.registrarAccion(
                "crear_cita",
                "Crea una cita médica automáticamente para el paciente actual",
                crearCitaHandler::crearCita
        );

        System.out.println("✅ Acciones IA registradas: listar_medicos_y_horarios, crear_cita");
    }
    }