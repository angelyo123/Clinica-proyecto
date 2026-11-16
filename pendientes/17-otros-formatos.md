# 17 — Otros formatos de plantilla (PDF)

**Fase:** 5 · Crecer · **Complejidad:** Compleja · **Depende de:** 06, 07
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Aceptar plantillas que no sean Word, sin cambiar el resto del sistema.

## Qué hay que hacer

- **Una interfaz común por formato:** leer el archivo y producir el esquema; recibir los valores y llenar el archivo.
- **PDF con campos de formulario (AcroForm):** leer los campos para armar el esquema y llenarlos con PDFBox, que ya está en el proyecto.
- **PDF escaneado o foto de un formato en blanco:**
  - ubicar los campos por visión, con coordenadas en la página;
  - llenarlos escribiendo texto sobre el PDF.

## Criterios de validación

- [ ] Una plantilla AcroForm y una escaneada recorren de punta a punta el mismo flujo que la de Word: esquema, captura, verificación y documento final.
