# 09 — Verificación y auditoría (API)

**Fase:** 2 · Núcleo backend · **Complejidad:** Mediana · **Depende de:** 07, 08
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Que ningún dato llegue al documento final sin que una persona lo confirme, y que quede registrado quién lo hizo.

## Qué hay que hacer

- **API** para listar los valores de una captura (primero los dudosos) y para confirmar un valor, corregirlo o marcarlo como "no aparece en la foto".
- **Estados del valor:** `sugerido` → `confirmado`, `corregido` o `vacio`. El valor sugerido y el valor final se guardan por separado.
- **Auditoría:** se registra quién, cuándo, el valor anterior y el nuevo, en una tabla donde solo se inserta.
- **Regla:** el documento final solo se genera cuando todos los campos obligatorios están verificados. En ese momento se llama al llenado de la tarea 07 y se guardan el docx y el PDF como versión de la HC.
- **Métricas:** porcentaje de campos corregidos por tipo y tiempo de verificación por captura (de la primera a la última acción).

## Referencias al código

- `entidades/CampoValor.java`: hoy solo guarda `valor`
- `entidades/HistoriaClinicaVersion.java`: versión de la HC
- `servicios/impl/HistoriaClinicaServiceImpl.java:54`: `guardarCampo`

## Criterios de validación

- [ ] Prueba: generar el documento con un campo obligatorio sin verificar devuelve error.
- [ ] Prueba: al corregir un valor, la auditoría guarda el valor sugerido, el final, el usuario y la fecha.
- [ ] Prueba: un usuario de otra clínica no puede ver ni verificar la captura.
- [ ] Prueba de punta a punta con lector simulado: de las fotos salen valores, se verifican y se pueden descargar el docx y el PDF.
