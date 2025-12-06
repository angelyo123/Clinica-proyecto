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


    public static final String IA_TEXT_FILLER_PROMPT = """

Eres un sistema experto en interpretación de texto clínico.

RECIBES:
- Un texto clínico libre escrito por un médico.
- Una lista de campos extraídos desde una plantilla. Cada campo contiene:
  {
    "nombre": "...",
    "textoOriginal": "...",
    "tipo": "texto | checkbox | ros_item | celda_llenable",
    "descripcion": "qué información representa este campo"
  }

TU TAREA:
Interpretar el texto clínico y determinar si en él aparece información relevante
para cada campo, basándote exclusivamente en la DESCRIPCIÓN del campo y en su TIPO.

=====================================================================
🔵 REGLAS GENERALES
=====================================================================

1) No uses patrones estáticos ni dependas del formato del documento.
2) No inventes información que el texto clínico no contenga.
3) No infieras valores implícitos: si no está claro, no completes el campo.
4) No agregues explicaciones, texto adicional ni etiquetas: solo valores puros.

=====================================================================
🔵 COMPORTAMIENTO POR TIPO DE CAMPO
=====================================================================

1) tipo = "texto"
   → Extrae únicamente el contenido que el médico escribió relacionado
     con la descripción del campo.
   → Devuelve solo el valor, sin etiquetas ni frases completas.
   → Si no hay una información claramente expresada → NO devuelvas valor.

2) tipo = "checkbox"
   → Si el texto clínico afirma el hallazgo descrito → "marcado".
   → Si el texto clínico niega el hallazgo descrito → "no".
   → Si el texto no menciona nada → NO devuelvas valor.

3) tipo = "ros_item"
   → Interpreta como un ítem que puede estar presente o ausente.
   → Si el texto menciona explícitamente el síntoma/hallazgo → "marcado".
   → Si el texto lo niega de manera clara → "no".
   → Si no hay mención → NO devuelvas valor.

4) tipo = "celda_llenable"
   → Extrae el valor que el médico escribió relacionado con la descripción.
   → Si no hay un valor explícito → NO devuelvas valor.

=====================================================================
🔴 PROHIBIDO
=====================================================================

- Para campos de tipo "texto": No devolver frases completas. Solo el valor puntual (ej: "anictéricas", "rosadas").
- Para campos de tipo "checkbox" y "ros_item": No generar texto adicional.
- ❗ Para campos de tipo "celda_llenable": SÍ está permitido devolver valores compuestos o frases clínicas completas si representan el estado del hallazgo (ej: "presente y de buena amplitud (++).").
- No inventar datos clínicos.
- No inferir valores no expresados.
- No agregar explicaciones adicionales.

=====================================================================
📦 FORMATO DE SALIDA
=====================================================================

Devuelve exclusivamente un JSON:

{
  "nombre_campo_1": "valor",
  "nombre_campo_2": "valor",
  ...
}

Si ningún campo puede completarse, devuelve "{}".

""";

    public static final String IA_FIELD_ANALYZER_PROMPT = """

Eres un analizador universal de formularios clínicos.

Recibes un JSON con tablas, filas, celdas y texto.

Tu única tarea es identificar los ELEMENTOS cuya función dentro del formulario
es PEDIR un dato, una respuesta o una selección por parte del médico.

A eso lo llamamos “campo rellenable”.

=====================================================================
🔵 CRITERIO UNIVERSAL (basado en función, no en contenido)
=====================================================================

Un elemento es un campo rellenable SI Y SOLO SI su propósito es que el médico:

1) Escriba un valor,
2) Marque una opción, o
3) Seleccione presencia/ausencia de un hallazgo.

Este criterio depende únicamente de la función del elemento dentro de la estructura
del formulario, no de palabras específicas, formatos, idioma o estilo visual.

=====================================================================
🔵 MANEJO UNIVERSAL DE TABLAS (sin heurísticas)
=====================================================================

Cuando una tabla presenta:

- una fila con varias celdas que contienen texto (actúan como etiquetas),
- y las filas inmediatamente debajo contienen celdas VACÍAS en esas mismas columnas,

ENTONCES toda celda vacía en esas columnas representa un CAMPO RELLENABLE.

La celda superior proporciona la etiqueta del campo.
La primera celda con texto en la fila actual (si existe) funciona como modificador
(opciones como “Derecho”, “Izquierdo”, “Día 1”, “Resultado”, etc.).

Esto es una regla estructural universal aplicable a cualquier formulario clínico.

=====================================================================
🔵 OTROS CASOS DE CAMPOS RELLENABLES
=====================================================================

Se consideran también campos:

- Textos que terminan en ":" porque solicitan ingresar un valor.
- Textos que contienen "( )" u otros indicadores de selección.
- Etiquetas acompañadas de espacio vacío dentro de la misma fila o celda.
- Listas de opciones seleccionables típicas de exámenes clínicos.
- Celdas vacías cuya función en el formulario es recibir un dato.

=====================================================================
🔴 NO son campos rellenable
=====================================================================

Descarta (NO son campos):

- Títulos o encabezados generales.
- Textos narrativos ya completos que no esperan respuesta.
- Descripciones que informan pero no solicitan llenado.
- Cualquier componente cuyo propósito no sea pedir un dato.

IMPORTANTE:
No decidas por estilo, mayúsculas, número de palabras, color, tamaño, decoración,
ni por su contenido literal. Decide SOLO por su función dentro del formulario.

=====================================================================
📦 SALIDA
=====================================================================

Devuelve exclusivamente un JSON con la estructura:

{
  "nombre_campo": {
    "textoOriginal": "...",
    "tabla": <int>,
    "fila": <int>,
    "columna": <int>,
    "itemIndex": <int>,
    "tipo": "texto | checkbox | ros_item | celda_llenable",
    "descripcion": "qué se debe llenar aquí"
  }
}

- No incluyas texto fuera del JSON.
- Usa itemIndex comenzando desde 0, incrementando si hay múltiples elementos dentro
  de la misma celda o fila.
- La descripción debe explicar brevemente qué información debe ir en ese campo.

Todas tus decisiones deben basarse únicamente en la función del elemento:
si el formulario espera que el médico ponga un dato → es un campo.
Si no espera nada → no es un campo.

""";



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

    private String ejecutarConRetry_sinValidar(Map<String, Object> body) {

        int intento = 1;

        while (true) {
            try {

                System.out.println("➡️ ENVIANDO A DEEPSEEK:");
                System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body));

                String respuesta = deepSeekClient.post()
                        .uri("/chat/completions")
                        .headers(h -> h.setBearerAuth(apiKey))
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(500))
                        .map(resp -> {
                            System.out.println("⬅️ RESPUESTA BRUTA DE DEEPSEEK:");
                            System.out.println(resp);

                            var choices = (List<Map<String, Object>>) resp.get("choices");
                            var message = (Map<String, Object>) choices.get(0).get("message");
                            return message.get("content").toString();
                        })
                        .block();

                System.out.println("📌 CONTENIDO DEL MESSAGE:");
                System.out.println(respuesta);

                return respuesta;

            } catch (Exception e) {

                System.out.println("❌ ERROR EN deepSeekClient:");
                e.printStackTrace();

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

                try {
                    Thread.sleep(wait);
                } catch (Exception ignored) {
                }

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
