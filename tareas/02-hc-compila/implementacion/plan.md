# Tarea 02 — Hacer que `hc` compile: plan de implementación

**Complejidad:** Mediana · **Rama:** `tarea/02-hc-compila` · **Estado:** Validada

## Objetivo

Que `backend-clinica/hc` compile, pase sus pruebas y arranque, recuperando el refactor completo que quedó en `stash@{1}`.

## Alcance

- **Incluye:** crear la rama, aplicar el stash, resolver el conflicto, corregir la ruta del docx en la prueba, compilar, probar y arrancar.
- **No incluye:**
  - cambiar las claves (tarea 01, en espera);
  - validar que los endpoints de IA den buenos resultados;
  - el selector de proveedor (tarea 21);
  - hacer commits sin tu autorización.

## Pasos

Un paso se marca `[x]` solo cuando su prueba pasó.

- [x] **Paso 1 — Crear la rama**
  - **Hacer:** `git switch -c tarea/02-hc-compila` desde `hc-digitalizacion-3`.
  - **Prueba (automática):** `git branch --show-current` → esperado: `tarea/02-hc-compila`.
  - **Resultado:** ✅ pasó (2026-11-16)

- [x] **Paso 2 — Aplicar `stash@{1}` y resolver el conflicto**
  - **Hacer:** `git stash apply stash@{1}`. Se resolvió `PlantillaProcessorService.java` con la versión del stash (`git checkout --theirs`) y se dejaron los cambios sin preparar para commit (`git reset`).
  - **Prueba (automática):**
    - `git diff stash@{1} -- backend-clinica/hc` → sin diferencias;
    - `git stash list` → el stash sigue existiendo.
  - **Resultado:** ✅ pasó (2026-11-16)

- [x] **Paso 3 — Corregir la ruta del docx en la prueba**
  - **Hacer:** en `src/test/java/.../visual/StableIdDeterminismTest.java:21`, se cambió `test_template.docx` (no existe) por `historia_clinica.docx`. También se agregaron dos comprobaciones: que se generen IDs y que ninguno sea `null`. Sin ellas, un documento sin tablas pasaría la prueba comparando dos conjuntos vacíos.
  - **Prueba (automática):** `./mvnw -o test -Dtest=StableIdDeterminismTest` → 1 prueba, 0 fallos.
  - **Resultado:** ✅ pasó (2026-11-16)

- [x] **Paso 4 — Compilar**
  - **Prueba (automática):** `./mvnw -o -q clean compile` en `backend-clinica/hc` → 0 errores.
  - **Resultado:** ✅ pasó (2026-11-16)

- [x] **Paso 5 — Correr todas las pruebas del módulo**
  - **Prueba (automática):** `./mvnw -o test` → `HcApplicationTests` 1/1 (con Postgres y LibreOffice locales) y `StableIdDeterminismTest` 1/1.
  - **Nota:** `TestCognitiveSerializer` tiene un `main()` y no `@Test`, así que Maven no lo ejecuta.
  - **Resultado:** ✅ pasó (2026-11-16)

- [x] **Paso 6 — Arrancar la aplicación**
  - **Prueba (manual):** `java -jar` con el Java 17 de `JAVA_HOME`, contra la BD local `hc-service`.
    - **Esperado:** `Started HcApplication` y el puerto 8088 responde.
    - **Obtenido:** arrancó en 8,2 s; LibreOffice conectó en los puertos 2002 y 2005; `GET /hc/plantilla/999999/poi` → 500, la respuesta esperada para una plantilla inexistente.
  - **Evidencia:** `evidencias/paso-6-arranque.txt`
  - **Resultado:** ✅ pasó (2026-11-16)

## Prueba de regresión

- [x] Solo cambiaron archivos de `backend-clinica/hc`: `git status --short` no muestra otros servicios, solo las carpetas nuevas `pendientes/` y `tareas/` → ✅
- [x] El stash sigue disponible como respaldo (`stash@{1}`, base `234c64b`) → ✅

## Registro de validación

| Fecha | Paso | Prueba | Resultado | Evidencia / detalle |
|---|---|---|---|---|
| 2026-11-16 | Previo | Compilar `stash@{1}` en una copia temporal (`git archive`) | ✅ 0 errores | Antes de tocar el repositorio |
| 2026-11-16 | Previo | `StableIdDeterminismTest` en la copia temporal | ❌ `FileNotFoundException: test_template.docx` | Motivo del paso 3 |
| 2026-11-16 | Previo | La misma prueba, con el docx renombrado en la copia temporal | ✅ 1/1 | — |
| 2026-11-16 | 1 | `git branch --show-current` | ✅ `tarea/02-hc-compila` | — |
| 2026-11-16 | 2 | `git stash apply stash@{1}` | ⚠️ 1 conflicto (`PlantillaProcessorService.java`), el esperado | Resuelto con la versión del stash |
| 2026-11-16 | 2 | `git diff stash@{1} -- backend-clinica/hc` y `git stash list` | ✅ sin diferencias; el stash sigue | — |
| 2026-11-16 | 3 | `./mvnw -o test -Dtest=StableIdDeterminismTest` | ✅ 1/1 | Con las comprobaciones nuevas |
| 2026-11-16 | 4 | `./mvnw -o -q clean compile` | ✅ 0 errores | — |
| 2026-11-16 | 5 | `./mvnw -o test` | ✅ 2/2 | `HcApplicationTests` y `StableIdDeterminismTest` |
| 2026-11-16 | 6 | `java -jar` con el `java` del PATH | ❌ `UnsupportedClassVersionError` | El PATH apunta a Java 8; es un problema del entorno, no del código |
| 2026-11-16 | 6 | `$JAVA_HOME/bin/java -jar` (Java 17), deteniendo enseguida | ⚠️ arrancó, pero hubo 2 errores de LibreOffice | Los causó el script al matar `soffice` antes de que conectara |
| 2026-11-16 | 6 | `$JAVA_HOME/bin/java -jar`, esperando 25 s | ✅ arrancó, LibreOffice conectado, 0 errores, puerto 8088 responde | `evidencias/paso-6-arranque.txt` |
