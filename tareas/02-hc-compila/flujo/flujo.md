# Tarea 02 — Hacer que `hc` compile: flujo

**Referencia:** Plan del producto HC (2026-11-16)
**Componentes afectados:** Backend (`backend-clinica/hc`) · Base de datos local de `hc`

Rutas relativas a `backend-clinica/hc/src/main/java/com/historias_clinicas/hc/`, salvo que se indique otra cosa.

## Antes (estado actual)

1. La rama `hc-digitalizacion-3` (HEAD `f263444`) no compila: unos 200 errores, la mayoría en cascada.
2. Las causas:
   - el commit `9445e64` borró `PlantillaAccion` y `PlantillaAccionRepo`, que todavía se usan (`servicios/PlantillaProcessorService.java:8,38` y `servicios/DocumentoService.java:6,11,23`);
   - 8 archivos declaran un `package` que no coincide con su carpeta (`cognitive/`, `extraction/`);
   - `visual/StableIdGenerator.java:5` declara la clase `generarIds`;
   - unas 20 clases están vacías: son esqueletos del refactor.
3. La implementación real del refactor nunca se subió a la rama. Quedó en `stash@{1}` (2026-05-09 20:36): 42 archivos de `hc`, +2960/−1728 líneas. Ese stash solo existe en esta computadora.
4. `application.properties:11` usa `ddl-auto=create-drop`, que borra las tablas en cada reinicio.

## Después

1. Rama nueva `tarea/02-hc-compila`, creada desde `hc-digitalizacion-3`.
2. Se aplica `stash@{1}` con `apply`, no con `pop`, para que el stash quede como respaldo. El único conflicto (`PlantillaProcessorService.java`) se resuelve con la versión del stash.
3. `StableIdDeterminismTest` apunta al `historia_clinica.docx` que existe, en lugar de `test_template.docx`.
4. `hc` compila y sus pruebas pasan. Los cambios quedan sin commit hasta que lo autorices.

```mermaid
flowchart LR
    A[hc-digitalizacion-3<br/>esqueletos vacíos<br/>no compila] --> B[rama tarea/02-hc-compila]
    S[(stash@{1}<br/>refactor completo)] -->|git stash apply| B
    B --> C[corregir ruta del docx<br/>en la prueba]
    C --> D[compila + pruebas + arranque]
```

## Qué cambia

| Aspecto | Antes | Después |
|---|---|---|
| Compilación de `hc` | Unos 200 errores | Sin errores |
| `PlantillaProcessorService` | 1535 líneas de heurísticas que no compilan | Unas 300 líneas: análisis, checklists, estructura POI y depuración del análisis "cognitive" |
| Clases `visual/`, `cognitive/`, `extraction/`, `fill/` | Vacías o con el package incorrecto | Implementadas |
| Endpoints | `/hc/*` y `/hc/plantilla/{subir, analizar, poi}` | Se suman `/hc/plantilla/{id}/cognitive-debug`, `/hc/version/{id}/ia/texto-preliminar`, `/hc/version/{id}/ia/imagen-preliminar`, `/hc/version/{id}/llenar-texto`, `/hc/version/plantilla/{id}/checklist` y `POST /fill` |
| Esquema de la BD | Se borra en cada reinicio (`create-drop`) | Se conserva y Hibernate lo actualiza (`update`) |
| Rama donde vive el trabajo | Solo en el stash local | En la rama `tarea/02-hc-compila` (sin commit todavía) |

## Impacto

| Área | Impacto | Acción necesaria |
|---|---|---|
| App móvil | No aplica: todavía no existe | — |
| Backend / API | Aparecen los endpoints nuevos del refactor. Los existentes se mantienen | Verificar que la app arranca y responde |
| Panel web | No aplica: `frontend-clinica` no llama a `hc` | — |
| Base de datos (estructura) | Entidades nuevas o cambiadas (`PlantillaSnapshot`, `PlantillaCampo`). Con `update`, Hibernate las crea o altera al arrancar | Usar solo la BD local `hc-service` |
| Datos existentes | No aplica: con `create-drop` la BD local se vaciaba en cada reinicio | — |
| Versiones de la app ya instaladas | No aplica | — |
| Integraciones con otros sistemas | Llamadas a DeepSeek y OpenAI desde los endpoints de IA. Estas pruebas no las llaman | No ejecutar endpoints de IA con datos reales |
| Usuario final / administrador | Ninguno: `hc` no tiene interfaz | — |
| Seguridad y datos personales | Sin cambios: las claves siguen en `application.properties` (tarea 01 en espera) y `hc` no tiene autenticación | Queda para las tareas 01 y 13 |

## Riesgos y reversión

- **Riesgo:** el stash era trabajo en curso; que compile no garantiza que se comporte bien. → **Mitigación:** compilar, correr las pruebas y arrancar la app. El comportamiento de la IA se valida en las tareas siguientes.
- **Riesgo:** perder el stash. → **Mitigación:** se usa `apply`, no `pop`, y el stash se borra solo cuando el trabajo tenga commit.
- **Cómo revertir:** `git checkout hc-digitalizacion-3` y `git branch -D tarea/02-hc-compila`. El stash sigue intacto.
