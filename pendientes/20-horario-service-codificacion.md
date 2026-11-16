# 20 — Horario-Service no compila (codificación)

**Fase:** fuera del producto HC · **Complejidad:** Simple · **Depende de:** —
**Referencia:** Análisis del repositorio (2026-11-16)

## Problema

`./mvnw compile` en `backend-clinica/Horario-Service` falla con `MalformedInputException` al copiar `application.properties`. El archivo tiene un carácter que no es UTF-8: la "ó" del comentario "Comunicación", guardada en Latin-1.

## Qué hay que hacer

- Volver a guardar `backend-clinica/Horario-Service/src/main/resources/application.properties` en UTF-8, o quitar la tilde del comentario.

## Criterios de validación

- [ ] `./mvnw -q compile` en `backend-clinica/Horario-Service` termina sin errores.
