# 21 — Elegir el proveedor de IA desde el front

**Fase:** 0 · Base estable · **Complejidad:** Mediana · **Depende de:** 02
**Referencia:** Pedido del usuario (2026-11-16)

## Objetivo

Cambiar desde el frontend qué API de IA usa `hc`, sin tocar código ni reiniciar el backend. Hoy DeepSeek está fijo en el código.

## Qué hay que hacer

- **Backend:** una interfaz `ProveedorIA` con operaciones de texto y de visión. Las primeras implementaciones son DeepSeek y OpenAI, sobre los clientes que ya existen (`DeepSeekClient`, `GptVisionClient`). Después se pueden sumar otras (Claude, Gemini).
- **Configuración guardada en la base de datos:**
  - proveedor y modelo activos, **separados por uso**: análisis de plantillas y lectura de fotos;
  - valores por defecto tomados de `application.properties`.
- **Endpoints:**
  - listar los proveedores disponibles (solo los que tienen clave configurada) y sus modelos, indicando cuáles aceptan imágenes;
  - consultar y cambiar la configuración activa.
- **Las claves se quedan en el backend.** El front solo elige proveedor y modelo: nunca ve ni envía claves.
- **Validar capacidades:** un modelo que no acepta imágenes no se puede asignar a la lectura de fotos.
- **Registrar** qué proveedor y modelo procesó cada análisis o captura, para poder comparar y auditar.
- **Front:** una pantalla de configuración en `frontend-clinica`, solo para administradores.
- **Privacidad:** cambiar de proveedor cambia a dónde van los datos de los pacientes. Ver la tarea 13.

## Referencias al código

- `config/DeepSeekConfig.java`: URL base y clave fijas de DeepSeek
- `ia/DeepSeekClient.java:109,163,483,550`: modelo `deepseek-chat` escrito directamente en el código
- `ia/gpt/GptVisionClient.java:34`: el modelo de OpenAI sí sale de una propiedad
- `ia/ImageInterpreter.java:15`: envía imágenes a DeepSeek
- `servicios/PlantillaProcessorService.java`: inyecta `DeepSeekClient` directamente
- `frontend-clinica/src/app/core/services/`: servicios del front

## Criterios de validación

- [ ] Prueba unitaria: con la configuración en OpenAI, el análisis llama a la implementación de OpenAI (simulada) y no a DeepSeek.
- [ ] Prueba: asignar a la lectura de fotos un modelo sin visión devuelve 400 con un mensaje claro.
- [ ] Prueba: la respuesta del listado de proveedores no contiene ninguna clave.
- [ ] Prueba: un usuario que no es administrador no puede cambiar la configuración (403).
- [ ] Manual: cambiar el proveedor en la pantalla y analizar una plantilla. El registro muestra el proveedor nuevo sin haber reiniciado el backend.
