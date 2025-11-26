package com.historias_clinicas.hc.ia;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextInterpreter {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Nuevo método: interpreta texto clínico y devuelve
     * un JSON donde cada campo está COMPLETADO según su textoOriginal.
     */
    public Map<String, Object> interpretarTexto(
            String texto,
            List<Map<String, Object>> camposPlantilla
    ) throws Exception {

        Map<String, Object> payload = Map.of(
                "texto", texto,
                "campos", camposPlantilla
        );

        String prompt = """
                Eres un sistema experto en historias clínicas.

                Recibirás:
                - Un texto clínico redactado por el médico.
                - Una lista de campos, donde cada campo tiene:
                     { "nombre": "...", "textoOriginal": "..." }

                Tu tarea:
                ✔ Buscar en el texto clínico los valores correspondientes.
                ✔ REESCRIBIR el textoOriginal agregando el valor correcto.
                ✔ Si no encuentras un valor claro, NO rellenes el campo.

                Ejemplo:
                textoOriginal: "Edad       :"
                texto clínico: "Paciente varón de 76 años..."
                salida: "Edad       : 76"

                Salida FINAL:
                {
                    "nombre_campo": "textoOriginal completado",
                    ...
                }

                NO devuelvas explicaciones.
                NO agregues nada fuera del JSON.
                """;

        // IA
        String raw = deepSeekClient.completarJSON_sinValidar(payload, prompt);

        // Sanitizar
        String sane = sanitize(raw);

        return mapper.readValue(sane, Map.class);
    }

    // --- Sanitizador ---
    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "{}";

        String s = raw.trim();
        int i = s.indexOf("{");
        int j = s.lastIndexOf("}");

        if (i < 0 || j <= i) {
            log.error("❌ DeepSeek no devolvió JSON válido");
            return "{}";
        }

        s = s.substring(i, j + 1);

        s = s.replaceAll("[\\u0000-\\u001F]", "");
        s = s.replaceAll(",\\s*}", "}");
        s = s.replaceAll(",\\s*]", "]");

        return s;
    }
}