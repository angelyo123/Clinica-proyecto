# 11 — App móvil: verificación

**Fase:** 3 · Aplicaciones · **Complejidad:** Compleja · **Depende de:** 09, 10
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Verificar una HC rápido y con confianza. Esta pantalla es el corazón del producto.

## Qué hay que hacer

- **Una tarjeta por campo** con:
  - el valor sugerido;
  - el recorte de la foto donde se leyó, con zoom;
  - los botones confirmar, corregir y "no aparece".
- **Orden:** primero los campos dudosos. El modo "revisar solo dudosos" se habilita únicamente si el piloto (tarea 15) demuestra que los campos "seguros" casi nunca fallan.
- **Entrada según el tipo de campo:** teclado de fecha o de número, y selector para opciones y casillas.
- **Vista previa** del documento final en PDF y envío.
- Hacer componentes reutilizables, para la vista web de revisiones largas (tarea 12).

## Criterios de validación

- [ ] Manual con 5 HC de la tarea 03: todos los campos se pueden confirmar o corregir, y el PDF final refleja las correcciones (evidencia).
- [ ] Medición: tiempo promedio para verificar una HC frente al tiempo de transcribirla a mano. Objetivo inicial: menos de un tercio.
- [ ] Manual: en al menos 9 de cada 10 campos revisados, el recorte mostrado corresponde al dato en la foto.
