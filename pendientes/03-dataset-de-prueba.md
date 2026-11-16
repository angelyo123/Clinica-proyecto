# 03 — Conjunto de prueba (plantilla + fotos + respuestas)

**Fase:** 1 · Validar lectura · **Complejidad:** Mediana · **Depende de:** cliente (plantilla y fotos)
**Referencia:** Plan del producto HC (2026-11-16)

## Problema

No sabemos si la IA lee bien la letra de las HC reales, y es el mayor riesgo del producto. Para medirlo hace falta un conjunto de prueba cuyas respuestas correctas ya conozcamos.

## Qué hay que hacer

- Elegir una plantilla real: la que más usa el cliente.
- Escribir a mano su **esquema de campos** en JSON. Cada campo lleva `id`, `etiqueta`, `tipo` (texto, fecha, número, casilla, opción, tabla), `opciones` y `seccion`. Este formato es el borrador del esquema de la tarea 06.
  ```json
  { "id": "antecedentes.alergias", "etiqueta": "Alergias", "tipo": "texto", "seccion": "Antecedentes" }
  { "id": "examen.edema", "etiqueta": "Edema", "tipo": "opcion", "opciones": ["Sí", "No"] }
  ```
- Reunir 30–50 fotos de HC llenadas, con variedad:
  - distintas letras, iluminación y teléfonos;
  - casos difíciles: casillas, tablas largas, tachones, páginas torcidas.
- **Privacidad:** lo mejor es que el personal llene HC en papel con datos ficticios. Si se usan HC reales, hay que anonimizarlas (tapar nombre, DNI y dirección) y tener permiso.
- Transcribir los valores correctos de cada HC en un archivo por historia (`respuestas/hc-01.json`), con las claves del esquema.
- Guardar el conjunto **fuera del repositorio**, en una carpeta local protegida, y anotar dónde quedó.

## Criterios de validación

- [ ] El esquema cubre todos los campos de la plantilla. Se revisa contra el Word, campo por campo.
- [ ] Hay al menos 30 HC con fotos y respuestas.
- [ ] Cada tipo de campo tiene al menos 20 ejemplos en total.
- [ ] Ninguna foto contiene datos reales sin consentimiento.

## Depende del cliente

- La plantilla y las fotos, con permiso de uso.
- Una persona que transcriba o valide las respuestas correctas.
