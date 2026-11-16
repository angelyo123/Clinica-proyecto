# 16 — Propuesta automática de esquema con IA

**Fase:** 5 · Crecer · **Complejidad:** Compleja · **Depende de:** 12, 15
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Reducir el tiempo de configurar una plantilla nueva. La IA propone el esquema y una persona lo confirma en el editor de la tarea 12.

## Qué hay que hacer

- **Terminar el enfoque de `CognitiveAnalysisService`:**
  - la IA recibe el documento completo (no bloques de 8 filas) y la imagen de cada página;
  - solo puede elegir stableIds que existen;
  - propone la etiqueta, el tipo, las opciones y la sección de cada campo.
- **Mostrar la propuesta** en el editor de la tarea 12 para que se confirme o corrija.
- **Medir contra el esquema hecho a mano:** porcentaje de campos encontrados, campos sobrantes y minutos de corrección.
- **Retirar `PlantillaProcessorService`** (las heurísticas congeladas) cuando la propuesta automática lo supere.

## Referencias al código

- `cognitive/CognitiveAnalysisService.java:23,38`: análisis y validación de IDs
- `cognitive/CognitivePromptBuilder.java`: el prompt actual es mínimo
- `servicios/PlantillaProcessorService.java:126`: flujo actual (heurísticas + DeepSeek por bloques)
- `ia/DeepSeekClient.java:86`: `conciliarRegion`

## Criterios de validación

- [ ] En 5 plantillas reales: se encuentra al menos el 90 % de los campos y la corrección humana toma menos de 15 minutos por plantilla.
- [ ] Prueba: la IA nunca logra introducir un stableId que no existe, porque la validación está activa.
