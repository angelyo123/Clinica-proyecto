# 04 — Experimento de lectura de fotos

**Fase:** 1 · Validar lectura · **Complejidad:** Mediana · **Depende de:** 03
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Decidir con datos si el producto es viable y con qué tecnología, antes de construir la app.

## Qué hay que hacer

- Escribir un script independiente en `experimentos/lectura-fotos/`, fuera de los servicios. Se sugiere Python por rapidez, pero sirve Java si lo prefieres.
- **Opción E1:** un modelo con visión recibe las fotos y el esquema, y devuelve JSON validado contra el esquema. Debe poder responder `null` ("no aparece") en cada campo. Probar 2–3 modelos (por ejemplo GPT-4o, Claude y Gemini).
- **Opción E2:** un OCR con coordenadas (Azure Document Intelligence o Google Document AI) más un LLM que asigna las palabras a los campos. El LLM debe citar los IDs de las palabras que usó, con el mismo patrón de IDs estables.
- Medir por tipo de campo y por opción:
  - **exactitud:** coincidencia exacta y después de normalizar (mayúsculas, espacios, formato de fecha);
  - **inventados:** valores que la IA dio donde la respuesta correcta es vacía;
  - **calibración:** cuántos campos que la IA marcó como "seguros" estaban mal;
  - **costo y tiempo** por HC.
- Escribir un informe corto con la tabla de resultados y la decisión.
- **Incluir DeepSeek con su modelo de visión.** Su guía oficial indica que no todos sus modelos aceptan imágenes: `deepseek-chat`, el que usa hoy el código, no está confirmado. Antes de probar, verificar en https://api-docs.deepseek.com/guides/vision cuál es el ID vigente del modelo con visión.

## Referencias al código

- `ia/ImageInterpreter.java:15`: hoy envía la imagen a `deepseek-chat`
- `ia/gpt/GptVisionClient.java:47`: cliente de visión que se puede reutilizar
- `ia/VisionPrompts.java`: el prompt actual está pensado para analizar la plantilla, no para leer datos
- `cognitive/CognitiveAnalysisService.java:38`: patrón de validación de IDs que se reutiliza en E2

## Criterios de validación

- [ ] El script corre dos veces y da resultados equivalentes, con la variación propia del modelo documentada.
- [ ] Hay una tabla de exactitud por tipo de campo y por opción.
- [ ] Hay una decisión escrita: si se sigue o no, qué opción y qué modelo, y desde qué nivel de confianza un campo se marca como "dudoso".

## Depende del cliente / de ti

- **Cliente:** la precisión mínima aceptable.
- **Tú:** crear las cuentas en los proveedores. Si se usan datos reales, deben ser cuentas con acuerdo para datos de salud.
