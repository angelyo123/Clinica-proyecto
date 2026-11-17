# Tarea 03 — Conjunto de prueba: flujo

**El flujo del producto no cambia.** La tarea arma material de prueba fuera del código: fotos de HC llenadas a mano, un esquema de campos y las respuestas correctas de cada foto.

**Cambio respecto a la ficha:** no hay cliente. Las fotos no vienen de una clínica ni de una plantilla propia; son 9 HC reales llenadas a mano que están publicadas en internet (Scribd, Studocu, SlideShare). Por eso no hay una plantilla única, y el esquema es **genérico**: 33 campos comunes a las HC (filiación, atención, enfermedad actual, funciones vitales, diagnóstico y plan). Cada formato usa solo los que tiene.

**Impacto:**

| Área | Impacto | Acción necesaria |
|---|---|---|
| App móvil, backend, panel web, base de datos | No aplica: no se toca el código del producto | — |
| Datos existentes | No aplica | — |
| Seguridad y datos personales | Las fotos muestran datos de pacientes reales que otras personas publicaron | Las imágenes y las transcripciones quedan en `.gitignore` y no salen de esta máquina. Solo se usaron con DeepSeek para el experimento (tarea 04). En git va la lista de fuentes |
| Validez del experimento | 9 fotos, de 9 formatos distintos, con la resolución que dan esos sitios (768×1024 la mayoría) | Los resultados son una primera señal, no una medición final. Ver "pendiente" en `implementacion/plan.md` |
