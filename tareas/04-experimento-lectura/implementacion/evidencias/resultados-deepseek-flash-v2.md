# Resultados: deepseek-flash, variante v2

Corridas: 1, 2. Las tablas por tipo y por documento son de la corrida 1.

## Resumen por corrida

| Corrida | Exactitud (campos con respuesta) | Inventados (campos vacíos que la IA llenó) | Seg. promedio por HC | Tokens promedio por HC | Costo de la corrida (USD) |
|---|---|---|---|---|---|
| 1 | 79% (55/70) | 6% (12/202) | 30.8 | 9280 | 0.06 |
| 2 | 84% (59/70) | 1% (2/202) | 40.1 | 10754 | 0.16 |

## Exactitud por tipo de campo

| Tipo | Exactitud | Omitidos | Inventados |
|---|---|---|---|
| edad | 71% (5/7) | 1 | 0% (0/1) |
| fecha | 73% (8/11) | 0 | 0% (0/5) |
| hora | 50% (1/2) | 0 | 0% (0/5) |
| nombre | 100% (4/4) | 0 | 10% (1/10) |
| numero | 100% (2/2) | 0 | 10% (5/52) |
| opcion | 100% (5/5) | 0 | — |
| telefono | 67% (2/3) | 0 | 0% (0/6) |
| texto | 92% (22/24) | 0 | 4% (4/95) |
| texto_largo | 50% (6/12) | 0 | 7% (2/28) |

## Por documento

| HC | Dificultad | Exactitud | Inventados | Excluidos (dudosos) |
|---|---|---|---|---|
| hc-01 | media | 79% (15/19) | 0% (0/12) | 2 |
| hc-02 | facil | 100% (8/8) | 0% (0/25) | 0 |
| hc-03 | facil | 100% (9/9) | 0% (0/23) | 1 |
| hc-04 | media | 100% (5/5) | 4% (1/26) | 2 |
| hc-05 | facil | 90% (9/10) | 0% (0/21) | 2 |
| hc-06 | dificil | 50% (4/8) | 0% (0/22) | 3 |
| hc-07 | media | 67% (2/3) | 0% (0/26) | 4 |
| hc-08 | extrema | 50% (1/2) | 36% (9/25) | 6 |
| hc-09 | dificil | 33% (2/6) | 9% (2/22) | 5 |

## Calibración (¿sirve la confianza que declara el modelo?)

| Confianza declarada | Campos con respuesta | Mal leídos o inventados |
|---|---|---|
| alta | 52 | 19% (10/52) |
| media | 23 | 48% (11/23) |
| baja | 6 | 83% (5/6) |

## Estabilidad entre corridas

Campos con el mismo valor en las corridas 1 y 2: 86% (254/297).

## Errores (sin valores, para no exponer datos de pacientes)

| HC | Campo | Tipo | Resultado | Confianza | Similitud o cobertura |
|---|---|---|---|---|---|
| hc-01 | paciente.grado_instruccion | texto | incorrecto | media | 0.84 |
| hc-01 | paciente.telefono | telefono | incorrecto | media |  |
| hc-01 | atencion.fecha | fecha | incorrecto | alta |  |
| hc-01 | atencion.hora | hora | incorrecto | alta |  |
| hc-04 | paciente.nombre | nombre | inventado | baja |  |
| hc-05 | atencion.fecha | fecha | incorrecto | alta |  |
| hc-06 | paciente.edad | edad | incorrecto | alta |  |
| hc-06 | paciente.fecha_nacimiento | fecha | incorrecto | media |  |
| hc-06 | clinica.relato | texto_largo | incorrecto | baja | 0.33 |
| hc-06 | clinica.diagnostico | texto_largo | incorrecto | baja | 0.0 |
| hc-07 | clinica.antecedentes | texto_largo | incorrecto | media | 0.5 |
| hc-08 | atencion.servicio | texto | inventado | media |  |
| hc-08 | clinica.motivo_consulta | texto_largo | inventado | media |  |
| hc-08 | clinica.tiempo_enfermedad | texto | inventado | alta |  |
| hc-08 | clinica.relato | texto_largo | incorrecto | media | 0.2 |
| hc-08 | clinica.antecedentes | texto_largo | inventado | media |  |
| hc-08 | vitales.presion_arterial | texto | inventado | alta |  |
| hc-08 | vitales.frecuencia_cardiaca | numero | inventado | alta |  |
| hc-08 | vitales.frecuencia_respiratoria | numero | inventado | alta |  |
| hc-08 | vitales.temperatura | numero | inventado | alta |  |
| hc-08 | vitales.saturacion | numero | inventado | alta |  |
| hc-09 | paciente.edad | edad | omitido | alta |  |
| hc-09 | clinica.motivo_consulta | texto_largo | incorrecto | media | 0.5 |
| hc-09 | clinica.tiempo_enfermedad | texto | incorrecto | media | 0.46 |
| hc-09 | clinica.forma_inicio | texto | inventado | baja |  |
| hc-09 | clinica.relato | texto_largo | incorrecto | baja | 0.38 |
| hc-09 | vitales.temperatura | numero | inventado | media |  |
