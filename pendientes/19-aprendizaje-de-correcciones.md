# 19 — Aprender de las correcciones

**Fase:** 5 · Crecer · **Complejidad:** Mediana · **Depende de:** 13, 15
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Usar las correcciones del personal para mejorar la lectura en cada clínica. Es una idea especulativa: no se sabe si mejora, hay que medirlo.

## Qué hay que hacer

- **Panel de métricas** por clínica, plantilla y campo: exactitud y campos más corregidos.
- **Ejemplos en el prompt (few-shot):** incluir correcciones recientes de la misma clínica. Esto envía más datos al proveedor, así que requiere la aprobación de la tarea 13.
- **Evaluar** con el conjunto de prueba antes y después del cambio.

## Criterios de validación

- [ ] La exactitud mejora de forma medible en el conjunto de prueba. Si no mejora, se descarta la idea.
