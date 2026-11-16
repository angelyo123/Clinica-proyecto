# 05 — Base del backend del producto

**Fase:** 2 · Núcleo backend · **Complejidad:** Compleja · **Depende de:** 01, 02
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Dejar lista la base técnica del producto antes de construir funciones: estructura por módulos, migraciones, multiclínica, almacenamiento de archivos y cola de trabajos.

## Qué hay que hacer

- **Estructura por módulos** dentro de `hc`: `plantillas`, `capturas`, `extraccion`, `verificacion`, `render` y `comun`. El código congelado queda aparte, en un paquete `legado`.
- **Migraciones con Flyway** en lugar de `ddl-auto=create-drop`, que hoy borra la base en cada reinicio.
- **Multiclínica:** entidad `Clinica` y `clinica_id` en todas las tablas. Cada consulta filtra por la clínica del usuario.
- **Autenticación:** validar en `hc` el JWT de `auth-service` y agregar los roles del producto (`ADMIN_CLINICA`, `MEDICO`, `ENFERMERA`, `OPERADOR`).
- **Almacenamiento de archivos:** una interfaz `AlmacenArchivos` con implementación en MinIO (S3 local). En la base solo se guardan la clave del archivo, su hash y los metadatos. Reemplaza los `byte[]` que hoy se guardan en la base.
- **Cola de trabajos en Postgres:** tabla `trabajo` (tipo, estado, intentos, error) y un worker que toma trabajos con `FOR UPDATE SKIP LOCKED` y reintenta los que fallan.
- **Docker Compose de desarrollo** con Postgres y MinIO.

## Referencias al código

- `backend-clinica/hc/src/main/resources/application.properties:11`: `ddl-auto=create-drop`
- `entidades/Plantilla.java:27`: `archivoOriginal` como `byte[]`
- `entidades/HistoriaClinicaVersion.java:30-36`: `wordFinal` y `pdfFinal` como `@Lob byte[]`

## Criterios de validación

- [ ] `docker compose up` levanta Postgres y MinIO, la app arranca y Flyway crea las tablas.
- [ ] Reiniciar la app no borra datos.
- [ ] Prueba de integración: un usuario de la clínica A no ve datos de la clínica B.
- [ ] Prueba: un trabajo encolado se procesa una sola vez aunque haya 2 workers. Si falla, se reintenta hasta N veces y queda en `error` con el motivo.
- [ ] Prueba: un archivo que se sube y se baja por `AlmacenArchivos` conserva el mismo hash.
