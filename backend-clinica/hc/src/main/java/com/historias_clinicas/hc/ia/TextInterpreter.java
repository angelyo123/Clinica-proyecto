package com.historias_clinicas.hc.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.dto.CeldaAnalizadaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextInterpreter {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Interpreta texto clínico usando:
     * - texto del médico
     * - nombres de campos detectados por POI
     * - celdas de tablas detectadas por POI (con coordenadas y palabras)
     *
     * La IA devuelve un JSON con:
     * - campos lineales:
     *      edad, pa, fc, relato, etc.
     * - celdas de tablas:
     *      t0_f1_c2, t1_f3_c1, ...
     */
    public Map<String, Object> interpretarTexto(
            String texto,
            List<String> camposPlantilla,
            List<CeldaAnalizadaDTO> celdasAnalizadas
    ) throws Exception {

        Map<String, Object> finalJson = new LinkedHashMap<>();

        // =========================================================
        // PARTE 1 — Campos lineales (sin tablas)
        // =========================================================
        Map<String, Object> payloadLineales = Map.of(
                "texto", texto,
                "campos", camposPlantilla
        );

        String promptLineales = """
        Eres un sistema experto en historias clínicas. Devuelve SOLO json.

        Procesa el texto y los nombres de campos proporcionados.

        Genera un JSON con los valores encontrados. SOLO campos cuyo valor exista.
        Ejemplo:
        { "edad": "73", "relato": "Paciente..." }

        No incluyas claves vacías.
        No incluyas campos con "".
        No inventes campos.
        """;

        Map<String, Object> jsonLineales =
                mapper.readValue(deepSeekClient.completarJSON(promptLineales, payloadLineales), Map.class);

        finalJson.putAll(jsonLineales);



        // =========================================================
        // PARTE 2 — Procesar por TABLAS
        // =========================================================
        Map<Integer, List<CeldaAnalizadaDTO>> porTabla = celdasAnalizadas.stream()
                .collect(Collectors.groupingBy(CeldaAnalizadaDTO::getTabla));

        for (Integer tablaIndex : porTabla.keySet()) {

            Map<String, Object> payloadTabla = Map.of(
                    "texto", texto,
                    "celdas", porTabla.get(tablaIndex),
                    "tabla", tablaIndex
            );

            String promptTabla = """
            Eres un sistema experto en historias clínicas.
            
            Esta TABLA contiene valores clínicos tipo "campo → valor".
            Devuelve SOLO json con claves t{tabla}_f{fila}_c{columna}.
            
            Reglas:
            - Si la celda es una ETIQUETA (PA:, FC:, FR:, SO2:, FiO2:, Peso:, Talla:, IMC:), NO la devuelvas.
            - Devuelve solo los VALORES.
            - Combina SO2 + % en un único valor ("75%").
            - FiO2 incluye unidad ("12 L/min").
            - NO inventes nada.
            
                    Identifica también estos valores aunque no existan en la lista de campos:
                    - temperatura
                    - saturacion
                    - fio2
                    - peso
                    - talla
            """;

            String respuestaCruda = deepSeekClient.completarJSON(promptTabla, payloadTabla);
            String soloJson = sanitize(respuestaCruda);

            Map<String, Object> jsonTabla =
                    mapper.readValue(soloJson, Map.class);


            finalJson.putAll(jsonTabla);
        }

        return finalJson;
    }


    // ============================================================
    // SANITIZADOR DE RESPUESTA (extrae SOLO el JSON)
    // ============================================================
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

        // Quitar caracteres invisibles
        s = s.replaceAll("[\\u0000-\\u001F]", "");

        // Arreglar comas colgantes
        s = s.replaceAll(",\\s*}", "}");
        s = s.replaceAll(",\\s*]", "]");

        return s;
    }
}