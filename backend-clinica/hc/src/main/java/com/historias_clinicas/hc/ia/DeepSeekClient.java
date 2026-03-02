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
    private static final Duration TIMEOUT = Duration.ofMinutes(30);
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

        String hint = (region != null)
                ? region.getOrDefault("hint_text", "sin_hint").toString()
                : "POI_ONLY";

        log.info("🧠 [DeepSeek] Conciliando región: {}", hint);

        try {

            String poiJson = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(estructuraPOI);

            String prompt = construirPromptConciliadorPorRegion(
                    null,   // 👈 VISIÓN NO USADA
                    poiJson
            );

            Map<String, Object> body = Map.of(
                    "model", "deepseek-chat",
                    "max_tokens", 8192,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String raw = ejecutarConRetry(body);
            String limpio = limpiarJSON(raw);

            if (limpio == null || limpio.isBlank()) {
                return List.of();
            }

            List<Map<String, Object>> acciones =
                    mapper.readValue(limpio, List.class);

            log.info("✅ Región '{}' conciliada: {} acciones",
                    hint,
                    acciones.size());

            return acciones;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error conciliando región: " + hint,
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
            String visionJson,
            String estructuraPoiJson
    ) {
        return """
Eres un sistema detector de campos editables basado SOLO en evidencia literal.

Entrada: JSON POI con tablas/filas/celdas y su texto EXACTO ("texto").
NO interpretes, NO infieras, NO asumas intención.
SOLO puedes devolver un campo si el textoOriginal contiene un patrón literal permitido.

============================================================
📘 POI_JSON
============================================================
%s

============================================================
✅ CRITERIO ÚNICO DE INCLUSIÓN (FILTRO DURO)
============================================================

Solo devuelve un objeto si textoOriginal cumple AL MENOS UNA condición:

(A) Contiene el carácter ":" en el mismo textoOriginal.
(B) Contiene paréntesis marcables literales: "( )" o "()" o "(   )"
    (paréntesis abiertos y cerrados en el textoOriginal).

Si NO cumple (A) ni (B) → NO lo devuelvas.

============================================================
📌 REGLA FILA–COLUMNA
============================================================

- La unidad editable es SIEMPRE la MISMA celda (misma tabla+fila+columna).
- NUNCA devuelvas la fila completa.
- NUNCA combines columnas.
- Si una fila tiene varias celdas elegibles, devuelve cada celda por separado.

============================================================
📌 REGLAS DE DESCRIPCIÓN (SEMÁNTICA CONTEXTUAL)
============================================================

La descripción debe explicar qué información representa el campo
considerando:

- El textoOriginal.
- La sección o encabezado inmediato superior.
- El contexto estructural dentro del documento.
- La tabla o bloque donde se encuentra.

Reglas:

- Integra el significado del textoOriginal con su contexto.
- Si el textoOriginal es genérico, utiliza la sección donde aparece
  para precisar su significado.
- No inventes información que no pueda deducirse del texto y su contexto inmediato.
- No menciones formato ni instrucciones técnicas.
- No menciones ":" ni paréntesis.
- No describas cómo se edita.
- La descripción debe ser una frase declarativa breve.
- No debe limitarse a repetir el textoOriginal.
- No debe incluir suposiciones clínicas no visibles en el documento.


Reglas:

- Integra el significado del textoOriginal con su contexto.
- Si el textoOriginal es genérico, utiliza la sección donde aparece
  para precisar su significado.
- No menciones formato ni instrucciones técnicas.
- No menciones ":" ni paréntesis.
- No describas cómo se edita.
- No inventes información fuera del contexto visible.
- Mantén la descripción clara, específica y profesional

============================================================
📦 SALIDA
============================================================

Devuelve EXCLUSIVAMENTE un JSON con una LISTA de objetos.
Cada objeto incluye EXACTAMENTE:
- tabla (int)
- fila (int)
- columna (int)
- textoOriginal (string EXACTO de la celda)
- accion ("EDITAR_CAMPO")
- descripcion (string)

NO agregues texto fuera del JSON.
""".formatted(estructuraPoiJson);
    }



    public static final String IA_TEXT_FILLER_PROMPT = """

Eres un sistema reescritor de campos documentales clínicos.

RECIBES:
1) Un TEXTO CLÍNICO libre escrito por un médico.
2) Una lista de CAMPOS extraídos desde una plantilla.

Cada campo contiene:
{
  "id": number,              // identificador único e inmutable
  "textoOriginal": string,   // texto actual del documento (puede contener solo rótulo o estar incompleto)
  "descripcion": string      // qué información representa este campo
}

TU TAREA:
Determinar si el TEXTO CLÍNICO contiene información relevante
para ese campo, basándote EXCLUSIVAMENTE en la DESCRIPCIÓN.

=====================================================================
📌 REGLA ABSOLUTA DE REESCRITURA
=====================================================================

- Si NO hay información relevante → NO devuelvas el campo.
- Si SÍ hay información relevante:

  → genera el TEXTO FINAL COMPLETO
    tal como debe quedar en el documento,
    usando el textoOriginal como base estructural
    y reemplazando completamente el contenido editable
    por la nueva información.

- El texto devuelto REEMPLAZA ÍNTEGRAMENTE
  el contenido actual del campo.

NO concatenes fragmentos.
NO devuelvas valores parciales.
NO respetes valores clínicos anteriores.
NO indiques cómo editar: devuelve el resultado final.

=====================================================================
🚫 PROHIBIDO
=====================================================================

- No inventar datos.
- No inferir información no escrita explícitamente.
- No devolver frases genéricas.
- No explicar nada.
- No devolver campos sin información clara.

=====================================================================
📦 FORMATO DE SALIDA
=====================================================================
Devuelve EXCLUSIVAMENTE un JSON.

Cada clave DEBE ser el identificador "id" EXACTO del campo recibido
en el JSON de entrada.

Cada valor DEBE ser el TEXTO FINAL COMPLETO que reemplazará
íntegramente el contenido actual de ese campo en el documento.

REGLAS ESTRICTAS:
- Usa EXCLUSIVAMENTE el campo "id" como clave del JSON de salida.
- NO uses "textoOriginal" como clave.
- NO uses la descripción como clave.
- NO inventes identificadores.
- NO devuelvas campos que no tengan información clara en el texto clínico.
- Si ningún campo aplica, devuelve un objeto JSON vacío: {}.
- NO agregues ningún texto fuera del JSON.


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


    public static final String IA_CHECKLIST_PROMPT = """
Eres un sistema de selección clínica estrictamente literal.

RECIBES:
1) Un TEXTO CLÍNICO.
2) Un CHECKLIST con opciones disponibles (cada opción tiene un id y un texto).

Tu tarea:
Seleccionar EXCLUSIVAMENTE las opciones cuyo texto esté
mencionado de forma literal y afirmativa dentro del texto clínico.

============================================================
📌 REGLAS CLÍNICAS ESTRICTAS
============================================================

- Solo marcar si el texto de la opción aparece explícitamente.
- Coincidencia debe ser literal o casi literal.
- NO usar sinónimos.
- NO usar equivalencias médicas.
- NO interpretar.
- NO expandir síntomas.
- NO completar síndromes.
- NO relacionar conceptos clínicos.
- NO inferir.
- NO deducir.
- NO asumir.

- Si una opción aparece negada (ej: “niega”, “sin”, “no presenta”), NO marcar.
- No marcar por similitud de palabras.
- No marcar por coincidencia parcial engañosa.
- No marcar por conocimiento médico externo.
- No marcar por probabilidad clínica.

Es completamente válido devolver lista vacía.

============================================================
📦 FORMATO DE SALIDA
============================================================

Devuelve EXCLUSIVAMENTE un JSON con esta estructura:

{
  "marcar": ["id1", "id2"]
}

- Usa EXCLUSIVAMENTE los id recibidos.
- No agregues texto.
- No agregues comentarios.
- No agregues campos adicionales.
- Si no aplica nada: { "marcar": [] }

NO agregues texto fuera del JSON.
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

    private String extraerContenidoDeepSeek(Map<String, Object> resp) {
        if (resp == null) {
            throw new RuntimeException("DeepSeek devolvió null (resp)");
        }

        Object choicesObj = resp.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            // aquí suelen venir respuestas tipo {"error":{...}} o similares
            throw new RuntimeException("DeepSeek sin 'choices'. Resp=" + resp);
        }

        Object choice0 = choices.get(0);
        if (!(choice0 instanceof Map<?, ?> c0)) {
            throw new RuntimeException("DeepSeek choices[0] no es Map. Resp=" + resp);
        }

        Object messageObj = c0.get("message");
        if (!(messageObj instanceof Map<?, ?> msg)) {
            throw new RuntimeException("DeepSeek sin 'message'. Resp=" + resp);
        }

        Object contentObj = msg.get("content");
        if (contentObj == null) {
            throw new RuntimeException("DeepSeek sin 'content'. Resp=" + resp);
        }

        return contentObj.toString();
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
                        .onStatus(
                                status -> status.is4xxClientError() || status.is5xxServerError(),
                                resp -> resp.bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .map(b -> new RuntimeException("DeepSeek HTTP " + resp.statusCode() + " body=" + b))
                        )
                        .bodyToMono(Map.class)
                        .timeout(TIMEOUT)
                        .map(this::extraerContenidoDeepSeek)
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
