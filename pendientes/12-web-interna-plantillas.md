# 12 — Web interna de plantillas

**Fase:** 3 · Aplicaciones · **Complejidad:** Mediana · **Depende de:** 06, 07
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Una herramienta interna para configurar las plantillas de clientes nuevos. Al principio la usas tú: tú haces la configuración de cada cliente.

## Qué hay que hacer

- Subir un docx y ver sus páginas renderizadas. La conversión Word → PDF → PNG ya existe.
- Ver la lista de celdas con su stableId y resaltarlas sobre la página.
- **Editor de esquema:**
  - crear y editar campos;
  - asignar el ancla de un campo haciendo clic en su celda;
  - editor JSON como alternativa.
- **Vista previa:** llenar con datos de ejemplo y ver el PDF resultante.
- Publicar la versión.
- **Vista web de verificación** para revisiones largas, reutilizando los componentes de la tarea 11.
- Decidir si se reutiliza `frontend-clinica` (Angular 20 + Tailwind) o se crea un proyecto aparte.

## Referencias al código

- `servicios/word/PdfToPngService.java:16`: páginas en PNG
- `frontend-clinica/`: frontend Angular existente

## Criterios de validación

- [ ] Manual: configurar desde cero la plantilla real de la tarea 03 en menos de 30 minutos (medido), con un PDF de vista previa correcto.
- [ ] Prueba: publicar sin campos, o con anclas repetidas, se rechaza.
