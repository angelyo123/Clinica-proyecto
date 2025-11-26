package com.historias_clinicas.hc.ia;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekClient {

    private final WebClient deepSeekClient;
    private final RestTemplate deepSeekRestTemplate;

    @Value("${deepseek.api-key}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    // ==============================
    // 🔵  CONFIG
    // ==============================
    private static final Duration TIMEOUT = Duration.ofSeconds(180);
    private static final int MAX_RETRIES = 3;


    // ============================================================
    // 1️⃣ COMPLETAR TEXTO (prompt simple)
    // ============================================================
    public String completar(String prompt) {

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        log.warn("🟦 [DeepSeek] completando prompt simple...");
        return ejecutarConRetry(body);
    }


    // ============================================================
    // 2️⃣ VISION: Entrada con IMAGEN
    // ============================================================
    public String completarImagen(String base64, String instrucciones) {

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of("type", "text", "text", instrucciones),
                                        Map.of("type", "image", "image_url", "data:image/jpeg;base64," + base64)
                                )
                        )
                )
        );

        log.warn("🟦 [DeepSeek] completando imagen/visión...");
        return ejecutarConRetry(body);
    }



    // ==================================================================================
    // 3️⃣ JSON MODE — Entrada gigante (bloques de estructura médica)
    // ==================================================================================
    public String completarJSON(String prompt, Map<String, Object> inputJson) {

        String jsonEntrada;
        try {
            jsonEntrada = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputJson);
        } catch (Exception e) {
            jsonEntrada = inputJson.toString();
        }

        log.warn("🟧 [DeepSeek] completando JSON gigante...");
        log.warn("📦 Tamaño del JSON de entrada: {} chars", jsonEntrada.length());

        String finalPrompt =
                prompt +
                        "\n\nIMPORTANTE: Devuelve SOLO JSON válido.\n" +
                        "NO expliques nada. NO agregues texto fuera del JSON.\n\n" +
                        "JSON de entrada:\n" +
                        jsonEntrada;

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                // Desactivar modo JSON duro — NO soporta prompts grandes
// "response_format": null
                "max_tokens", 4096,
                "messages", List.of(Map.of("role", "user", "content", finalPrompt))
        );

        return ejecutarConRetry(body);
    }


    public String completarJSON_sinValidar(Map<String, Object> payload, String prompt) {

        String jsonEntrada;

        try {
            jsonEntrada = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (Exception e) {
            jsonEntrada = payload.toString();
        }

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "max_tokens", 4096,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content",
                                prompt +
                                        "\n\nA continuación tienes el JSON de entrada:" +
                                        "\n" + jsonEntrada
                        )
                )
        );

        return ejecutarConRetry_sinValidar(body);
    }


    public String completarJSON_sinValidar(Map<String, Object> inputJson) {

        String jsonEntrada;

        try {
            jsonEntrada = mapper.writeValueAsString(inputJson);
        } catch (Exception e) {
            jsonEntrada = inputJson.toString();
        }

        String prompt = """
                Eres un sistema experto en análisis de plantillas médicas en Word.
                
                Tu tarea es detectar EXCLUSIVAMENTE los CAMPOS RELLENABLES presentes en la estructura (JSON) enviada.
                
                Un “campo rellenable” es cualquiera de los siguientes patrones:
                - espacios en blanco grandes
                - líneas como: ____ , _________, ____________
                - casilleros vacíos: (  ), (   ), [ ], [   ], { }
                - casilleros tipo opción: “Murphy (  )”, “Mc Burney (  )”
                - frases con huecos para completar: “Motivo de consulta: __________”
                - tablas donde se espera que el médico escriba datos
                - campos con líneas punteadas o espacios múltiples
                - cualquier texto diseñado para que el médico escriba información
                
                NO debes devolver:
                - secciones conceptuales (“ingreso”, “examen físico”, “diagnóstico”, etc.)
                - campos ya llenos con texto real
                - texto explicativo, conclusiones o interpretaciones médicas
                - inventar campos basados en tu conocimiento clínico
                - contenido fuera del JSON
                
                Tu salida DEBE SER EXACTAMENTE un JSON con este formato:
                
                {
                  "nombre_campo": {
                      "textoOriginal": "el texto exacto donde aparece el hueco",
                      "parrafo": <indexParrafo o null>,
                      "tabla": <indexTabla o null>,
                      "fila": <indexFila o null>,
                      "columna": <indexColumna o null>
                  },
                  ...
                }
                
                Reglas adicionales:
                - Usa nombres_campo simples y en snake_case.
                - Si un bloque no contiene campos rellenables, devuelve {}.
                - Si ves varios campos en una misma línea, devuélvelos como campos separados.
                - No agregues nada fuera del JSON final.
                - SIN Backtick Symbol
                Ahora analiza el JSON siguiente y devuelve SOLO los campos rellenables en el formato pedido.
        """;

        // ❗ SIN response_format, DeepSeek no se "ahorca"
        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "max_tokens", 4096,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt + "\n\nJSON:\n" + jsonEntrada)
                )
        );

        return ejecutarConRetry_sinValidar(body);
    }

    private String ejecutarConRetry_sinValidar(Map<String, Object> body) {

        int intento = 1;

        while (true) {
            try {

                String respuesta = deepSeekClient.post()
                        .uri("/chat/completions")
                        .headers(h -> h.setBearerAuth(apiKey))
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(150))
                        .map(resp -> {
                            var choices = (List<Map<String, Object>>) resp.get("choices");
                            var message = (Map<String, Object>) choices.get(0).get("message");
                            return message.get("content").toString();
                        })
                        .block();

                return respuesta;

            } catch (Exception e) {
                if (intento >= 3) throw new RuntimeException(e);
                intento++;
            }
        }
    }



    // ==================================================================================
    // ⚡ MÉTODO CENTRAL: EJECUCIÓN CON RETRIES + LOGS + TIMEOUT
    // ==================================================================================
    private String ejecutarConRetry(Map<String, Object> body) {

        int intento = 1;

        while (true) {
            try {
                log.warn("🔄 [DeepSeek] Intento {}/{}", intento, MAX_RETRIES);

                long inicio = System.currentTimeMillis();

                String respuesta = deepSeekClient.post()
                        .uri("/chat/completions")
                        .headers(h -> h.setBearerAuth(apiKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(TIMEOUT)
                        .map(resp -> {
                            var choices = (List<Map<String, Object>>) resp.get("choices");
                            var message = (Map<String, Object>) choices.get(0).get("message");
                            return message.get("content").toString();
                        })
                        .block();

                long ms = System.currentTimeMillis() - inicio;

                log.warn("🟩 [DeepSeek] Respuesta completa en {} ms", ms);
                log.warn("📥 RAW (primeros 600 chars):\n{}",
                        respuesta != null && respuesta.length() > 600
                                ? respuesta.substring(0, 600)
                                : respuesta);

                // Validar JSON (si es modo JSON)
                validarJSON(respuesta);

                return respuesta;

            } catch (Exception e) {

                log.error("❌ [DeepSeek] Error: {}", e.getMessage());

                if (intento >= MAX_RETRIES) {
                    log.error("⛔ [DeepSeek] Reintentos agotados!");
                    throw new RuntimeException("DeepSeek falló: " + e.getMessage(), e);
                }

                long wait = (long) (Math.pow(2, intento) * 500L);
                log.warn("⏳ Esperando {} ms antes del retry...", wait);

                try { Thread.sleep(wait); } catch (Exception ignored) {}

                intento++;
            }
        }
    }



    // ==================================================================================
    // 🧠 VALIDACIÓN DE JSON DEVUELTO POR deepseek
    // ==================================================================================
    private void validarJSON(String contenido) throws Exception {

        if (contenido == null) {
            throw new RuntimeException("Respuesta nula de DeepSeek.");
        }

        contenido = contenido.trim();

        if (!contenido.startsWith("{") || !contenido.endsWith("}")) {
            log.warn("⚠ JSON inválido o truncado detectado:\n{}", contenido);
            throw new RuntimeException("DeepSeek devolvió JSON inválido o incompleto.");
        }

        // Parseo real para validar
        mapper.readTree(contenido);
    }
}
