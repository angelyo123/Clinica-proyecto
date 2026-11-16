# 01 — Sacar las claves del repositorio

**Fase:** 0 · Base estable · **Complejidad:** Mediana · **Depende de:** —
**Referencia:** Plan del producto HC (2026-11-16)
**Estado:** En espera por decisión del usuario (2026-11-16): por ahora no se mueven las claves.

## Problema

Los `application.properties` versionados en git contienen:
- claves reales de DeepSeek y OpenAI;
- las contraseñas de la base de datos;
- el secreto JWT, que es el mismo en 5 servicios.

El repositorio está en GitHub (`origin`), así que hay que dar esas claves por expuestas.

## Qué hay que hacer

- En todos los servicios, reemplazar los valores sensibles por variables de entorno (`${DEEPSEEK_API_KEY}`, `${OPENAI_API_KEY}`, `${DB_PASSWORD}`, `${JWT_SECRET}`).
- Crear un archivo local no versionado con los valores (`.env` o `application-local.properties`) y un ejemplo versionado sin valores (`.env.example`).
- Agregar esos archivos al `.gitignore`.
- Generar un secreto JWT nuevo, largo y aleatorio.
- **Rotar las claves de DeepSeek y OpenAI** (lo haces tú, en sus paneles).
- Decidir si se limpia el historial de git con `git filter-repo` o BFG. Reescribe la historia y obliga a hacer `push --force`. Aunque se limpie, las claves hay que rotarlas igual.

## Referencias al código

- `backend-clinica/hc/src/main/resources/application.properties:7,9,15`: contraseña de BD, clave de DeepSeek y clave de OpenAI
- `backend-clinica/Automatizacion-Service/src/main/resources/application.properties:7,13`: contraseña de BD y clave de DeepSeek
- `jwt.secret` y `spring.datasource.password` en los `application.properties` de auth-service, Pacientes, Médicos, Citas y Horario

## Criterios de validación

- [ ] `git grep -lE "sk-[A-Za-z0-9]{10,}"` no lista ningún archivo en HEAD.
- [ ] Cada servicio arranca con las variables definidas y, si falta una, falla con un mensaje claro.
- [ ] Las claves viejas ya no funcionan: devuelven 401 en DeepSeek y OpenAI.

## Depende de ti

- Rotar las claves.
- Decidir si se limpia el historial.
