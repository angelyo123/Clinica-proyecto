# Tarea 03 — Conjunto de prueba: plan de implementación

**Complejidad:** Mediana · **Rama:** `tarea/03-dataset-de-prueba` · **Estado:** Implementación

## Objetivo
Tener fotos reales de HC llenadas a mano, un esquema de campos y las respuestas correctas de cada foto, para medir la lectura con IA (tarea 04).

## Alcance
- **Incluye:** buscar y elegir las fotos en internet, el esquema genérico, la transcripción de las respuestas y la protección de los datos personales.
- **No incluye:** fotos propias ni de una clínica, ni el esquema de una plantilla concreta (eso es la tarea 06).

## Pasos

Un paso se marca `[x]` solo cuando su prueba pasó.

- [x] **Paso 1 — Buscar y elegir fotos de HC llenadas a mano**
  - **Hacer:** se buscó en Bing Imágenes (unas 600 imágenes, sobre todo de Scribd, Studocu y SlideShare), se revisaron en hojas de miniaturas y se ampliaron las dudosas. Quedaron 9 en `dataset/imagenes/`; las fuentes están en `dataset/fuentes.md`.
  - **Prueba (manual):** abrir cada foto ampliada y confirmar que los datos están escritos a mano. Se descartaron una que imitaba la letra con una fuente de computadora, una escrita casi toda a máquina y una nota suelta.
  - **Resultado:** ✅ pasó (2026-11-16): 9 fotos manuscritas de 6 países (Perú, México, Ecuador, Argentina, Honduras) y 8 formatos distintos.

- [x] **Paso 2 — Esquema de campos**
  - **Hacer:** `dataset/esquema.json`, con 33 campos. Cada uno tiene `id`, `etiqueta`, `tipo` y `seccion`, y `opciones` cuando corresponde. Es el borrador del esquema de la tarea 06.
  - **Prueba (automática):** script de validación (en el registro): el JSON es válido y cada campo tiene las claves obligatorias.
  - **Resultado:** ✅ pasó (2026-11-16).

- [x] **Paso 3 — Respuestas correctas**
  - **Hacer:** un archivo `dataset/respuestas/hc-NN.json` por foto, con los campos que su formato tiene. Los tachados o difuminados van en `null` a propósito, para medir si la IA inventa. Los que ni la transcripción humana lee con seguridad van con `dudoso: true` y no se miden. Los textos largos se miden por **claves**: frases cortas que se leen con seguridad.
  - **Prueba (automática):** el mismo script verifica que solo se usan ids del esquema y que cada campo tiene `valor` o `claves`.
  - **Resultado:** ✅ pasó (2026-11-16). 67 campos con respuesta, 5 en `null` a propósito y 24 dudosos excluidos.

- [x] **Paso 4 — Proteger los datos personales**
  - **Hacer:** `.gitignore` para `imagenes/` y `respuestas/`, y para las salidas crudas del experimento.
  - **Prueba (automática):** `git check-ignore -v` sobre una imagen, una respuesta y una salida → las tres están ignoradas.
  - **Resultado:** ✅ pasó (2026-11-16).

## Criterios de la ficha que no se cumplen

| Criterio | Estado | Motivo y qué falta |
|---|---|---|
| Al menos 30 HC con fotos y respuestas | ❌ 9 | Casi todo lo publicado son formatos en blanco o llenados en computadora. Para llegar a 30 hacen falta fotos propias, por ejemplo HC llenadas a mano con datos ficticios |
| Cada tipo de campo con 20 ejemplos | ❌ | Fechas 11, texto 23, edad 7, sexo 5, nombre 4, teléfono 3, hora 2, número 2, texto largo 10 |
| Ninguna foto con datos reales sin consentimiento | ❌ | Son documentos reales publicados por terceros. Se mitigó: no salen de esta máquina ni van a git |
| El esquema cubre la plantilla campo por campo | No aplica | No hay plantilla única; el esquema es genérico |
| Las respuestas las valida una persona | ⏳ | Las transcribió Claude ampliando cada imagen. **Revísalas tú**, sobre todo hc-01, hc-06 y hc-09 |

Por eso la tarea queda en **Implementación**: sirve para la primera medición (tarea 04), pero no alcanza para una decisión final.

## Registro de validación

| Fecha | Paso | Prueba | Resultado | Evidencia / detalle |
|---|---|---|---|---|
| 2026-11-16 | 1 | Revisión manual de unas 600 miniaturas y ampliación de las dudosas | ✅ 9 fotos | `dataset/fuentes.md` |
| 2026-11-16 | 1 | Ampliación de hc-03 (antigua) | ❌ no estaba escrita a mano: era una fuente de computadora | Se sacó del conjunto |
| 2026-11-16 | 2–3 | Script de validación del esquema y las respuestas | ✅ 33 campos, 9 archivos, 67 campos con respuesta | Salida en la conversación de la sesión |
| 2026-11-16 | 4 | `git check-ignore -v` | ✅ las tres rutas están ignoradas | — |
