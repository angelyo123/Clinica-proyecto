# Tarea 04 — Experimento de lectura de fotos: plan de implementación

**Complejidad:** Mediana · **Rama:** `tarea/03-dataset-de-prueba` (se trabajó junto con la 03) · **Estado:** Implementación

## Objetivo
Medir si un modelo con visión lee bien HC llenadas a mano y decidir cómo seguir antes de construir la extracción (tarea 08).

## Alcance
- **Incluye:** la opción E1 con `deepseek-flash` en dos variantes, la comparación automática contra las respuestas de la tarea 03 y el informe.
- **No incluye:** la opción E2 (OCR con coordenadas), porque no hay cuentas de Azure ni de Google; tampoco GPT, Claude ni Gemini (GPT queda fuera por decisión tuya y para los otros no hay claves).

## Pasos

- [x] **Paso 1 — Script del experimento**
  - **Hacer:** `experimentos/lectura-fotos/leer_fotos.py`, con dos comandos:
    - `correr`: manda cada foto con el esquema y guarda el JSON crudo en `salidas/`, que está en `.gitignore`;
    - `evaluar`: compara contra las respuestas y escribe el resumen en `evidencias/`.
  - **Prueba (automática):**
    - aserciones sobre los comparadores (fechas con mes en letras, horas a.m./p.m., edad en meses, teléfono con espacios, nombre en otro orden) → pasan;
    - una foto de prueba (hc-02) → 8 de 8 campos correctos.
  - **Resultado:** ✅ pasó (2026-11-16).

- [x] **Paso 2 — Correr las dos variantes, dos veces cada una**
  - **Hacer:** v1 manda la foto completa. v2 manda la foto completa más sus dos mitades ampliadas.
  - **Prueba (automática):** las 36 llamadas devuelven JSON válido con los 33 campos.
  - **Resultado:** ✅ pasó (2026-11-16): 0 errores de API y 0 JSON inválidos.

- [x] **Paso 3 — Evaluar y revisar los errores a mano**
  - **Hacer:** `evaluar` por variante. Después se revisó cada error contra la imagen ampliada, para separar los errores del modelo de los de la transcripción.
  - **Prueba (manual):** cada "incorrecto" o "inventado" se contrastó con la imagen ampliada. Se corrigieron 4 criterios de la transcripción, que aplican igual a las dos variantes (ver registro). Los demás son errores reales del modelo.
  - **Evidencia:** `evidencias/resultados-deepseek-flash-v1.md` y `evidencias/resultados-deepseek-flash-v2.md`. No tienen valores, para no exponer datos de pacientes.
  - **Resultado:** ✅ pasó (2026-11-16).

- [x] **Paso 4 — Decisión escrita**
  - **Resultado:** ✅ ver "Resultados y decisión" más abajo. Es **preliminar**: con 9 fotos, un campo más o menos mueve la exactitud unos 1.5 puntos.

## Resultados y decisión

### Resumen (campos con respuesta: 70; campos que debían quedar vacíos: 202)

| Variante | Exactitud corrida 1 / 2 | Inventados corrida 1 / 2 | Seg. por HC | Estabilidad entre corridas |
|---|---|---|---|---|
| **v1: foto completa** | 76% / 80% | **0.5% / 0.5%** | ~37 | 86% de campos iguales |
| v2: completa + mitades ampliadas | 79% / 84% | 6% / 1% | ~35 | 86% |

**Costo:** USD 0.25 en total por 39 llamadas, contando las de prueba; el saldo bajó de 1.50 a 1.25. Son unos **USD 0.007 por página**. Las corridas se hicieron en paralelo, así que la columna de costo por corrida de los informes no es confiable; vale el total.

### Lo más importante: depende del tipo de documento

| Tipo de documento | Exactitud v1 | Exactitud v2 | Inventados v1 | Inventados v2 |
|---|---|---|---|---|
| Formulario impreso con datos a mano en casillas o renglones (hc-01, 02, 03, 05, 07) | **88–90%** | **88–94%** | 0–1% | 0% |
| Hoja libre con letra cuidada (hc-04) | 100% | 100% | 0% | 0–4% |
| Notas en cursiva o letra rápida (hc-06, 08, 09) | 31–44% | 44–50% | 0–1% | **3–16%** |

### Errores que importan para el producto

- **Lecturas equivocadas con confianza "alta":**
  - En fechas y números: 28/04 → 24/04, 07/07 → 02/07, un dígito del teléfono, 08:30 → 09:30.
  - Con confianza "alta", v1 se equivoca en el 9% de los campos y v2 en el 19%. **La confianza no basta para saltarse la verificación.**
- **Confabulación clínica:**
  - En notas difíciles el modelo "completa" con lo que suena médicamente plausible. En hc-09 leyó "39 6/6" (semanas de embarazo) como "fiebre de 39.5 °C", y en hc-06 escribió "Esquizofrenia" donde dice "Episodio depresivo".
  - En hc-08, una imagen de 468×640, v2 inventó un cuadro completo (cefalea, 120/80, 88 lpm, 36.5 °C) con confianza "alta".
- **Lo que sí funciona bien:**
  - Respeta lo tachado y lo difuminado: no inventó nombres, salvo una inicial con confianza "baja".
  - Lee casillas marcadas, por ejemplo el estado civil.
  - En formularios con letra legible acierta casi todo.

### Decisión (preliminar)

1. **Seguir con el producto.** Para formularios impresos llenados a mano, la lectura ronda el 90% con casi nada inventado. Alcanza para un producto de **transcripción asistida**: la IA propone y una persona verifica.
2. **Usar v1 (foto completa).** v2 acierta un poco más, pero inventa bastante más en notas difíciles. Inventar es peor que dejar vacío, porque obliga a desconfiar de todo.
3. **La verificación humana es obligatoria y debe mostrar evidencia:** junto a cada campo, el recorte de la foto de donde salió (tareas 09 y 11). Las fechas, los números y los textos clínicos se confirman siempre, aunque la confianza sea "alta".
4. **Marcar como "dudoso"** todo campo con confianza "media" o "baja" (30–100% de error) y todo texto largo.
5. **Empezar por formularios estructurados.** Las notas de evolución en cursiva (31–50%) no están listas: tratarlas como "transcripción libre a revisar" o dejarlas para después.
6. **Para la tarea 08**, conviene probar E2: OCR con coordenadas más un LLM que cite los IDs de las palabras. Eso permite mostrar la evidencia y reduce la confabulación, porque el modelo solo puede usar palabras que existen.

### Pendiente para cerrar la tarea

- Repetir con **30 o más fotos**. Lo ideal son fotos propias con celular, de HC llenadas a mano con datos ficticios, y en el formato que vaya a usar el producto. El script ya está listo: basta con agregar imágenes y respuestas en `tareas/03-dataset-de-prueba/dataset/`.
- **Revisar las transcripciones** de la tarea 03: las hizo Claude.
- Probar E2 cuando haya una cuenta de OCR.
- Decidir la precisión mínima aceptable por campo. Antes era una pregunta para el cliente; ahora la decides tú.

## Registro de validación

| Fecha | Paso | Prueba | Resultado | Evidencia / detalle |
|---|---|---|---|---|
| 2026-11-16 | — | Claves del repositorio contra `/models` | ❌ DeepSeek y OpenAI responden 401 | Se usó una clave de prueba tuya, guardada fuera del repositorio |
| 2026-11-16 | — | Foto de prueba con `deepseek-flash` y `deepseek-v4-pro` | flash ✅ lee la foto; v4-pro ❌ descarta la imagen sin avisar (155 tokens de entrada) y devuelve todo en null | Importante para la tarea 21: validar que el modelo elegido realmente lee imágenes |
| 2026-11-16 | 1 | Aserciones de los comparadores + hc-02 | ✅ 8/8 | — |
| 2026-11-16 | 2 | 4 corridas × 9 fotos | ✅ 36/36 respuestas JSON válidas | — |
| 2026-11-16 | 3 | Revisión manual de errores | Se corrigieron 4 criterios: hc-07 tiene casillas de patologías marcadas; hc-09 tiene motivo y tiempo de enfermedad dentro de la nota; en hc-06 el nombre de la clínica no cuenta como "servicio"; en hc-05 se quitaron claves que eran etiquetas del formulario | Las notas de cada cambio están en `respuestas/*.json` |
| 2026-11-16 | 3 | Evaluación final | v1: 76% / 80% de exactitud, 0.5% inventados. v2: 79% / 84%, 6% / 1% | `evidencias/resultados-deepseek-flash-v1.md`, `-v2.md` |
