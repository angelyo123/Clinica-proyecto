# 02 — Hacer que `hc` compile

**Fase:** 0 · Base estable · **Complejidad:** Mediana · **Depende de:** —
**Referencia:** Plan del producto HC (2026-11-16)

## Problema

`./mvnw compile` en `backend-clinica/hc` falla por cuatro causas:
1. El commit `9445e64` borró `PlantillaAccion` y `PlantillaAccionRepo`, pero el código todavía los usa.
2. 8 archivos declaran un `package` distinto al de su carpeta (en `cognitive/` y `extraction/`).
3. `visual/StableIdGenerator.java` declara la clase `generarIds`.
4. Hay unas 20 clases vacías, que son esqueletos de un refactor a medias.

## Hallazgo (2026-11-16): el código que compila está en un stash

`stash@{1}` ("WIP on hc-digitalizacion-3", 2026-05-09 20:36) contiene la implementación completa del refactor:
- 42 archivos, +2960/−1728 líneas;
- llena todos los esqueletos vacíos (`visual/`, `cognitive/`, `extraction/`, `fill/`, `HCIAController`, `NaturalLanguageFillController`);
- reduce `PlantillaProcessorService` en unas 1500 líneas;
- cambia `ddl-auto` de `create-drop` a `update`.

Resultados en una copia temporal:
- **Compila sin errores.**
- `StableIdDeterminismTest` pasa, pero solo después de apuntarlo a `historia_clinica.docx`: el test busca `test_template.docx`, que no existe.

Si se aplica sobre la rama actual, habrá **un único conflicto**, en `PlantillaProcessorService.java`. Se resuelve quedándose con la versión del stash.

**Atención:** el stash solo existe en esta computadora, no en GitHub.

Compilación de las otras versiones de `hc`:

| Versión | Fecha | ¿Compila? |
|---|---|---|
| `stash@{1}` | 2026-05-09 | ✅ |
| `stash@{2}`, `stash@{3}` (sobre `aa8dd39`) | 2025-12 | ✅ (versiones antiguas) |
| `HC-DIGITALIZACION` (`c8e446a`), `00909d6`, `aa8dd39`, `1ab74d7` | 2025-12 a 2026-01 | ✅ |
| `71152ca`, `ae231f8`, `9445e64`, `f263444` (HEAD) | 2026-02 a 2026-05 | ❌ |

Las demás ramas no tienen la carpeta `hc`.

**Nuevo enfoque de la tarea:** recuperar el stash en vez de corregir a mano lo que se describe abajo.

## Qué hay que hacer (enfoque original, si no se usa el stash)

- Restaurar `PlantillaAccion` y `PlantillaAccionRepo` desde `9445e64^` (`git show 9445e64^:<ruta>`). Así el flujo antiguo compila y queda congelado como referencia.
- Corregir el `package` de los 8 archivos para que coincida con su carpeta, y ajustar los imports.
- Renombrar la clase `generarIds` a `StableIdGenerator`.
- Conservar solo los esqueletos vacíos que se van a usar en las tareas 05 a 09; borrar el resto.
- Darles contenido real a las pruebas: hoy `StableIdDeterminismTest` y `TestCognitiveSerializer` no tienen ningún `@Test`.

## Referencias al código

- `servicios/PlantillaProcessorService.java:8,38`: usan `PlantillaAccionRepo`, que ya no existe
- `servicios/DocumentoService.java:6,11,23`: usan `PlantillaAccion` y `PlantillaAccionRepo`
- `cognitive/CognitiveAnalysisService.java`, `cognitive/CognitivePromptBuilder.java`, `cognitive/CognitiveSerializer.java`: package incorrecto
- `extraction/dto/FieldValueAI.java`, `extraction/dto/NaturalLanguageFillRequest.java`, `extraction/prompt/FieldExtractionPromptBuilder.java`, `extraction/service/FieldExtractionService.java`, `extraction/validation/FieldValueValidator.java`: package incorrecto
- `visual/StableIdGenerator.java:5`: `public class generarIds`
- `backend-clinica/hc/src/test/resources/historia_clinica.docx`: documento de prueba

## Criterios de validación

- [ ] `./mvnw -q compile` en `backend-clinica/hc` termina sin errores.
- [ ] `./mvnw test` pasa, y `StableIdDeterminismTest` comprueba que procesar dos veces `historia_clinica.docx` da los mismos IDs.
- [ ] La aplicación arranca (`./mvnw spring-boot:run`) contra la base de datos local.
