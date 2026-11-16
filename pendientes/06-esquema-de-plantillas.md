# 06 — Esquema de campos de las plantillas

**Fase:** 2 · Núcleo backend · **Complejidad:** Mediana · **Depende de:** 03 (borrador del formato), 05
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Representar **qué datos pide una plantilla** sin depender de su formato. Hoy se guarda solo una posición (tabla, fila, columna), sin tipo ni significado.

## Qué hay que hacer

- Modelo de entidades:
  - **`Plantilla` → `PlantillaVersion`:** archivo original, archivo preparado, hash y estado (`borrador` o `publicada`).
  - **`CampoPlantilla`:**
    - identificación: ID estable y clave legible (`antecedentes.alergias`);
    - contenido: etiqueta, tipo, opciones, sección, si es obligatorio y sus validaciones;
    - **ancla:** el stableId de la celda o del párrafo en el documento.
- Documentar el formato JSON del esquema y validarlo con JSON Schema.
- API para crear una plantilla, subir una versión, cargar o editar su esquema y publicarla. **Una versión publicada no se modifica:** los cambios van en una versión nueva.
- Evolucionar o reemplazar los actuales `PlantillaCampo` y `PlantillaSeccion`.

## Referencias al código

- `entidades/PlantillaCampo.java:43-53`: ubicación por índices (`indexTabla`, `indexFila`, `indexCelda`)
- `controladores/PlantillaController.java:23`: endpoint actual para subir plantillas

## Criterios de validación

- [ ] Prueba unitaria: un esquema con un tipo inválido, un ID repetido o una opción vacía se rechaza con un mensaje claro.
- [ ] Prueba de integración: crear la plantilla, cargar el esquema de la tarea 03 y publicar. Después, intentar editar la versión publicada debe dar error.
- [ ] Una HC creada con la versión 1 sigue apuntando a la versión 1 después de publicar la versión 2.
