# 08 — Extracción desde fotos

**Fase:** 2 · Núcleo backend · **Complejidad:** Compleja · **Depende de:** 04 (tecnología elegida), 05, 06
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

A partir de las fotos de una HC, obtener un valor sugerido para cada campo del esquema, junto con la evidencia de dónde se leyó y su nivel de confianza.

## Qué hay que hacer

- **Entidades:**
  - `Captura`: versión de la plantilla, identificador del paciente, quién la creó y su estado (`subida → procesando → por_verificar → verificada`, o `error`).
  - `Foto`: orden, clave en el almacenamiento y hash.
- **Endpoint de subida** de varias fotos. Crea un trabajo en la cola y responde sin esperar a la IA.
- **Interfaz `LectorDocumento`** para que el proveedor sea intercambiable. La primera implementación es la que salga elegida en la tarea 04.
- **Preprocesado de cada foto:** orientación, tamaño y compresión.
- **Salida estructurada** validada contra el esquema, con evidencia:
  - Cada valor cita las palabras o regiones de la foto de donde salió.
  - Un valor sin evidencia válida se marca como sospechoso.
- **Validadores por tipo** (fecha válida, DNI de 8 dígitos, rangos de signos vitales) y las señales de confianza que defina la tarea 04.
- **Guardado:** cada `ValorCampo` queda en estado `sugerido`, con su confianza y su evidencia (foto y región).
- **Reprocesar** una captura no duplica valores. Se registran el costo y el tiempo de cada captura.

## Referencias al código

- `ia/gpt/GptVisionClient.java:47`: cliente de visión
- `cognitive/CognitiveAnalysisService.java:38`: validación de IDs que se reutiliza para la evidencia

## Criterios de validación

- [ ] Prueba de integración con un lector simulado: subir 3 fotos genera un trabajo, y los valores quedan guardados como `sugerido`.
- [ ] Prueba: si el proveedor falla, el trabajo se reintenta y, después de N intentos, la captura queda en `error` con el motivo.
- [ ] Prueba: un valor que cita una palabra que no existe queda marcado como sospechoso.
- [ ] Con 10 HC de la tarea 03 y el proveedor real, la exactitud está a 5 puntos o menos de la del experimento.
- [ ] El endpoint de subida responde en menos de 2 s, y el estado se consulta aparte.
