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
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final Duration TIMEOUT = Duration.ofSeconds(360);
    private static final int MAX_RETRIES = 3;


    // ============================================================
// 🧩 CONCILIACIÓN POR BLOQUES (REEMPLAZA completar())
// ============================================================
    public List<Map<String, Object>> conciliarEstructuraPorBloques(
            Map<String, Object> estructuraPOI,
            Map<String, Object> vision
    ) {

        log.info("🧠 [DeepSeek] Conciliación iniciada (Vision → POI)");

        String raw = conciliarEstructura(estructuraPOI, vision);
        String limpio = limpiarJSON(raw);

        try {
            List<Map<String, Object>> acciones =
                    mapper.readValue(limpio, List.class);

            log.info("🏁 Conciliación finalizada: {} acciones", acciones.size());
            return acciones;

        } catch (Exception e) {
            throw new RuntimeException("Error parseando acciones conciliadas", e);
        }
    }

    private String limpiarJSON(String raw) {

        if (raw == null || raw.isBlank()) return "[]";

        String txt = raw.trim()
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .replaceAll("^[^\\[{]+", "")
                .replaceAll("[^\\]}]+$", "");

        if (!txt.startsWith("[") && !txt.startsWith("{")) {
            txt = "[" + txt + "]";
        }

        return txt;
    }



    // ============================================================
// 🧩 CONCILIAR UNA SOLA REGIÓN VISUAL (ANTI-TRUNCAMIENTO)
// ============================================================
    public List<Map<String, Object>> conciliarRegion(
            Map<String, Object> estructuraPOI,
            Map<String, Object> region
    ) {

        log.info("🧠 [DeepSeek] Conciliando región: {}",
                region.getOrDefault("hint_text", "sin_hint"));

        try {

            String regionJson = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(region);

            String poiJson = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(estructuraPOI);

            String prompt = construirPromptConciliadorPorRegion(
                    regionJson,
                    poiJson
            );

            Map<String, Object> body = Map.of(
                    "model", "deepseek-chat",
                    "max_tokens", 8192, // 👈 más que suficiente por región
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String raw = ejecutarConRetry(body);
            String limpio = limpiarJSON(raw);

            // 🔒 Protección dura contra JSON truncado
            if (!limpio.trim().endsWith("]")) {
                throw new IllegalStateException(
                        "JSON truncado al conciliar región: " +
                                region.getOrDefault("hint_text", "?")
                );
            }

            List<Map<String, Object>> acciones =
                    mapper.readValue(limpio, List.class);

            log.info("✅ Región '{}' conciliada: {} acciones",
                    region.getOrDefault("hint_text", "?"),
                    acciones.size());

            return acciones;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error conciliando región: " +
                            region.getOrDefault("hint_text", "?"),
                    e
            );
        }
    }

    // ============================================================
// 🔹 CONCILIAR UN SOLO BLOQUE (BASE DE TODO)
// ============================================================
    private String conciliarEstructura(
            Map<String, Object> estructuraPOI,
            Map<String, Object> vision
    ) {

        try {
            String visionJson = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(vision.get("regiones"));

            String poiJson = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(estructuraPOI);

            String prompt = construirPromptConciliador(
                    visionJson,
                    poiJson
            );

            Map<String, Object> body = Map.of(
                    "model", "deepseek-chat",
                    "max_tokens", 8192,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            return ejecutarConRetry(body);

        } catch (Exception e) {
            throw new RuntimeException("Error construyendo prompt conciliador", e);
        }
    }


    private String construirPromptConciliadorPorRegion(
            String regionJson,
            String estructuraPoiJson
    ) {

        return """
Eres un sistema conciliador entre UNA REGIÓN VISUAL
y la estructura física de un documento extraída mediante POI.

La región visual es la ÚNICA fuente de decisión.
La estructura POI es SOLO un mapa para localizar posiciones reales.

============================================================
📕 REGIÓN VISUAL A PROCESAR
============================================================

Esta solicitud corresponde EXCLUSIVAMENTE
a la siguiente región visual.

NO debes procesar otras regiones.
NO debes crear instrucciones nuevas.

REGION_JSON:
%s

============================================================
📘 ESTRUCTURA FÍSICA DEL DOCUMENTO (POI)
============================================================

Contiene tablas, filas y celdas con texto real
y posiciones físicas.

Úsalo SOLO para localizar las celdas
descritas por la región visual.

POI_JSON:
%s

============================================================
🚫 REGLAS ABSOLUTAS
============================================================

- Procesa EXCLUSIVAMENTE esta región.
- El campo "accion" debe devolverse EXACTAMENTE
  con el valor recibido en la región.
- NO inventes acciones.
- NO cambies el tipo de acción.
- NO inventes texto clínico.
- NO incluyas celdas no descritas por la región.

============================================================
🎯 TU TAREA
============================================================

1) Lee la acción indicada en la región.
2) Usa la descripción estructural para localizar
   la celda o conjunto de celdas correspondientes en POI.
3) Genera UNA o MÁS acciones con coordenadas reales.

============================================================
📦 FORMATO DE SALIDA
============================================================

Devuelve EXCLUSIVAMENTE un JSON con una LISTA de acciones.

Cada acción debe incluir:
- tabla
- fila
- columna (o columna inicial si aplica)
- textoOriginal
- accion (EXACTAMENTE igual a la región)
- descripcion

NO agregues texto fuera del JSON.
""".formatted(regionJson, estructuraPoiJson);
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

    private String construirPromptConciliador(
            String visionJson,
            String estructuraPoiJson
    ) {

        return """
Eres un sistema conciliador entre un ANÁLISIS VISUAL YA REALIZADO
y la estructura física de un documento extraída mediante POI.

El análisis visual (VISION) es la ÚNICA fuente de decisiones.
La estructura POI es SOLO un mapa para localizar posiciones reales.

============================================================
📕 ÓRDENES VISUALES (VISION)
============================================================

Este objeto contiene TODAS las instrucciones que debes ejecutar.
NO debes crear instrucciones nuevas.
NO debes modificar ni reinterpretar las instrucciones existentes.

VISION_JSON:
%s

============================================================
📘 ESTRUCTURA FÍSICA DEL DOCUMENTO (POI)
============================================================

Este objeto contiene tablas, filas y celdas con texto real
y posiciones físicas.

Debes usarlo ÚNICAMENTE para localizar coordenadas reales
indicadas por las órdenes visuales.

POI_JSON:
%s

============================================================
🚫 REGLAS ABSOLUTAS
============================================================

- Procesa EXCLUSIVAMENTE las órdenes contenidas en VISION_JSON.
- El campo "accion" debe devolverse EXACTAMENTE
  con el mismo valor recibido en Vision.
- NO inventes acciones.
- NO cambies el tipo de acción.
- NO inventes texto clínico.
- NO analices POI si no existe una orden visual asociada.

============================================================
🎯 TU TAREA
============================================================

Para CADA orden visual:

1) Lee la acción indicada en Vision.
2) Usa la descripción para localizar en POI
   la celda o conjunto de celdas correspondientes.
3) Genera UNA o MÁS acciones con coordenadas reales.

============================================================
📦 FORMATO DE SALIDA
============================================================

Devuelve EXCLUSIVAMENTE un JSON con una LISTA de acciones.

Cada acción debe incluir:
- tabla
- fila
- columna (o columna inicial si abarca varias)
- textoOriginal
- accion (EXACTAMENTE igual a Vision)
- descripcion

NO agregues texto fuera del JSON.
""".formatted(visionJson, estructuraPoiJson);
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


    private List<Map<String, Object>> dividirEnBloques(
            Map<String, Object> estructuraPOI,
            Map<String, Object> vision,
            int maxChars
    ) {

        List<Map<String, Object>> bloques = new ArrayList<>();

        List<Map<String, Object>> tablas =
                (List<Map<String, Object>>) estructuraPOI.get("tablas");

        List<Map<String, Object>> regiones =
                (List<Map<String, Object>>) vision.getOrDefault("regiones", List.of());

        for (Map<String, Object> tabla : tablas) {

            Map<String, Object> bloque = new LinkedHashMap<>();
            bloque.put("tabla", tabla);
            bloque.put("vision", regiones);

            int size;
            try {
                size = mapper.writeValueAsString(bloque).length();
            } catch (Exception e) {
                size = Integer.MAX_VALUE;
            }

            if (size <= maxChars) {
                bloques.add(bloque);
            } else {
                // fallback ultra seguro: tabla sola
                bloques.add(Map.of("tabla", tabla));
            }
        }

        return bloques;
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
                "max_tokens", 8192,
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

            String respuesta = null; // 👈 CLAVE

            try {
                log.warn("🔄 [DeepSeek] Intento {}/{}", intento, MAX_RETRIES);

                long inicio = System.currentTimeMillis();

                respuesta = deepSeekClient.post()
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

                log.warn("🟩 [DeepSeek] Respuesta recibida en {} ms ({} chars)",
                        ms,
                        respuesta != null ? respuesta.length() : 0);

                if (log.isDebugEnabled()) {
                    log.debug("📥 [DeepSeek] RAW COMPLETO:\n{}", respuesta);
                }

                String limpio = limpiarJSON(respuesta);
                validarJSON(limpio);

                return respuesta;

            } catch (Exception e) {

                log.error("❌ [DeepSeek] Error en intento {}: {}", intento, e.getMessage());

                if (log.isDebugEnabled() && respuesta != null) {
                    log.debug("🧨 [DeepSeek] RAW FALLIDO:\n{}", respuesta);
                }

                if (intento >= MAX_RETRIES) {
                    log.error("⛔ [DeepSeek] Reintentos agotados!");
                    throw new RuntimeException("DeepSeek falló", e);
                }

                long wait = (long) (Math.pow(2, intento) * 500L);
                log.warn("⏳ Esperando {} ms antes del retry...", wait);

                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ignored) {}

                intento++;
            }
        }
    }


    // ==================================================================================
    // 🧠 VALIDACIÓN DE JSON DEVUELTO POR deepseek
    // ==================================================================================
    private void validarJSON(String contenido) throws Exception {

        if (contenido == null || contenido.isBlank()) {
            throw new RuntimeException("Respuesta nula de DeepSeek.");
        }

        contenido = contenido.trim();

        if (!(contenido.startsWith("{") || contenido.startsWith("["))) {
            throw new RuntimeException("JSON inválido");
        }

        mapper.readTree(contenido);
    }

}
