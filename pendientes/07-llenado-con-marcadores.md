# 07 — Llenado con marcadores (sin IA)

**Fase:** 2 · Núcleo backend · **Complejidad:** Compleja · **Depende de:** 02, 06
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Llenar el Word del cliente de forma predecible y sin IA, conservando su formato.

Hoy pasa lo contrario:
- se borra el texto de la celda y se reescribe, con lo que se pierde el formato;
- existe un servicio que le pregunta a la IA cómo reescribir cada celda.

## Qué hay que hacer

- **Preparar la plantilla una sola vez.** Para cada campo del esquema, insertar un marcador `{{clave}}` en una copia del docx, en la ubicación que indica su ancla (stableId). Las etiquetas originales ("Nombre:") no se tocan.
- **Casillas:** un marcador que se reemplaza por ☒ o ☐ (o un control de contenido de casilla de Word).
- **Tablas con filas variables:** un marcador de fila que se repite. poi-tl lo soporta.
- **Llenado:** reemplazar los marcadores por los valores verificados, con poi-tl o con un reemplazo propio que respete los fragmentos de texto con formato de Word (runs).
- Convertir a PDF con el `WordToPdfService` existente.
- Retirar el llenado celda por celda: `AiRenderService` y `WordRenderEngine.escribirCeldaCompleta`.

## Referencias al código

- `servicios/word/WordRenderEngine.java:13`: borra los runs y reescribe la celda
- `servicios/word/AiRenderService.java:17`: le pide a la IA el texto de cada celda
- `servicios/word/PlantillaFillService.java:16`: llenado actual
- `servicios/word/WordToPdfService.java:17`: conversión a PDF (se reutiliza)
- `visual/StableIdGenerator.java`: IDs que sirven de ancla

## Criterios de validación

- [ ] Prueba automática con `historia_clinica.docx`: preparar y llenar con valores de prueba. El texto del docx resultante contiene cada valor y cada etiqueta original.
- [ ] Prueba: el formato de las etiquetas (negrita, fuente, tamaño) es igual antes y después. Se comparan las propiedades de los runs.
- [ ] Prueba: llenar dos veces con los mismos valores produce el mismo contenido.
- [ ] Revisión visual del PDF de la plantilla real de la tarea 03 (evidencia: una imagen PNG por página).
- [ ] Hay un comportamiento definido y probado para un valor más largo que su celda.
