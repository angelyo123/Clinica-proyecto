# 14 — Despliegue

**Fase:** 4 · Piloto · **Complejidad:** Mediana · **Depende de:** 05 (puede avanzar en paralelo con la fase 3)
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Un ambiente de pruebas (staging) y uno de producción que se puedan reconstruir siguiendo instrucciones.

## Qué hay que hacer

- **Imagen Docker del backend** con LibreOffice incluido, porque JODConverter lo necesita para convertir a PDF.
- **Un solo gestor de LibreOffice.** Hoy hay dos: `generadores/PdfGenerator.java:25` (puerto 2002) y `servicios/word/JodConverterConfig.java:16` (puerto 2005, con `officeHome` fijo en `C:/Program Files/LibreOffice`). Hay que dejar uno, y que la ruta y el puerto salgan de propiedades para que funcione en Linux.
- **Infraestructura** con Docker Compose o un servicio gestionado: backend, Postgres y almacenamiento de objetos.
- **Variables y secretos** por ambiente, siguiendo la tarea 01.
- **Backups** automáticos de la base de datos y de los archivos, con una prueba de restauración.
- **Logs y alertas básicas:** errores en los trabajos y costo diario de IA.
- **Distribución de la app para el piloto:** APK interno o TestFlight.

## Criterios de validación

- [ ] Staging se despliega desde cero siguiendo solo el README, sin pasos manuales sin documentar.
- [ ] Al restaurar un backup en otro ambiente, las HC y sus archivos aparecen completos.
- [ ] Una falla del proveedor de IA genera una alerta visible.
