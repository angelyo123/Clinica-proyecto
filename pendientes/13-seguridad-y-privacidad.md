# 13 — Seguridad y privacidad

**Fase:** 4 · Piloto · **Complejidad:** Compleja · **Depende de:** 05 (puede avanzar en paralelo con la fase 3)
**Referencia:** Plan del producto HC (2026-11-16)

## Objetivo

Poder trabajar con datos reales de pacientes de forma legal y segura. Las HC son de los datos más sensibles que existen, y es lo primero que preguntará una clínica antes de comprar.

## Qué hay que hacer

- **Proveedor de IA y OCR:**
  - con acuerdo para el tratamiento de datos de salud;
  - que no entrene con los datos;
  - con una región de almacenamiento aceptable para el cliente.
- **Base legal:** definir el consentimiento con la institución y revisar las normas locales. En Perú serían la Ley 29733 de Protección de Datos Personales y la norma técnica del MINSA sobre historia clínica. Confirmarlo con asesoría legal.
- **Cifrado:** en tránsito (HTTPS) y en reposo (base de datos y archivos).
- **Permisos:** roles por clínica y registro de accesos (quién vio qué HC).
- **Retención:** cuánto tiempo se guardan las fotos después de generar el documento, con borrado automático.
- **Endpoints:** ninguno con `security.disabled=true` ni abierto sin necesidad.
- **Minimizar lo que se envía a la IA:** solo lo necesario para leer los campos.

## Criterios de validación

- [ ] Prueba: un endpoint sin token devuelve 401, y con el token de otra clínica devuelve 403.
- [ ] Prueba: las fotos se borran automáticamente al cumplirse el plazo definido.
- [ ] Hay un documento con el proveedor elegido, sus condiciones de datos y la política de retención, aprobado por el cliente.

## Depende del cliente

- Sus requisitos legales e institucionales.
- La aprobación del documento de privacidad.
