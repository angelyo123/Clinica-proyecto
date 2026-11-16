# 18 — Alineación cuando la foto es del mismo formato

**Fase:** 5 · Crecer · **Complejidad:** Compleja · **Depende de:** 04, 08 · **Solo aplica si** las fotos son la versión impresa de la misma plantilla
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Mejorar la precisión aprovechando que se sabe dónde está cada campo en la página.

## Qué hay que hacer

1. Renderizar la plantilla como imagen y guardar la región de cada campo.
2. Alinear la foto con esa imagen. La técnica se llama "registro de formularios": se buscan puntos característicos y se calcula una homografía, que es la transformación que "endereza" la foto para que calce con la plantilla. Se hace con OpenCV, que tiene versión para Java (JavaCV) y para Python.
3. Recortar la región de cada campo y leerla por separado. Ese recorte también sirve como evidencia en la pantalla de verificación.

## Criterios de validación

- [ ] Con el conjunto de la tarea 03, la exactitud mejora frente a la opción elegida en la tarea 04. Hay que definir antes cuánto debe mejorar para que valga la pena (por ejemplo, 5 puntos).
