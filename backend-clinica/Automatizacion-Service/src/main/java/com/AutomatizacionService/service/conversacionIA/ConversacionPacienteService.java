package com.AutomatizacionService.service.conversacionIA;
import com.AutomatizacionService.service.conversacionIA.context.ConversacionCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConversacionPacienteService {

    @Autowired private ConversacionCoreService coreService;

    public Map<String, Object> procesarConversacion(Map<String, Object> solicitud) {
        return coreService.ejecutarFlujoConversacion(solicitud);
    }
}