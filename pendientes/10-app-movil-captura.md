# 10 — App móvil: captura

**Fase:** 3 · Aplicaciones · **Complejidad:** Compleja · **Depende de:** 05, 08
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Que una enfermera o un médico capture una HC en papel en menos de un minuto, incluso sin señal.

## Qué hay que hacer

- **Proyecto nuevo** `movil-hc/` con Ionic + Capacitor y Angular, para aprovechar lo que ya sabes de Angular.
- Inicio de sesión con el JWT de la tarea 05, y selección de la plantilla y del identificador del paciente.
- **Escáner de documentos nativo** (ML Kit en Android, VisionKit en iOS):
  - detecta los bordes y corrige la perspectiva;
  - admite varias páginas, que se pueden reordenar o eliminar.
- **Modo sin conexión:**
  - las capturas se guardan cifradas dentro de la app;
  - se suben cuando hay red, con reintentos.
- **Las fotos nunca pasan por la galería** y se borran del teléfono cuando se confirma la subida.
- Lista de capturas con su estado: subiendo, procesando o por verificar.

## Criterios de validación

- [ ] Manual en un Android real: capturar 3 páginas en modo avión y luego activar la red. La captura llega al backend (evidencia: capturas de pantalla y el registro en la BD).
- [ ] Manual: después de subir, ni la galería ni la carpeta de la app contienen las fotos.
- [ ] Manual: cerrar la app a mitad de la subida y volver a abrirla. La subida continúa sin duplicar la captura.
- [ ] `ng test` pasa para los servicios de la cola y de la subida.

## Depende del cliente

- Modelos de teléfono y sistema operativo (Android y/o iOS).
- Cómo es la conectividad en planta.
