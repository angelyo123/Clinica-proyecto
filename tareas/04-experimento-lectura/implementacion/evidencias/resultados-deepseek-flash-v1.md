# Resultados: deepseek-flash, variante v1

Corridas: 1, 2. Las tablas por tipo y por documento son de la corrida 1.

## Resumen por corrida

| Corrida | Exactitud (campos con respuesta) | Inventados (campos vacíos que la IA llenó) | Seg. promedio por HC | Tokens promedio por HC | Costo de la corrida (USD) |
|---|---|---|---|---|---|
| 1 | 76% (53/70) | 0% (1/202) | 38.7 | 8956 | 0.09 |
| 2 | 80% (56/70) | 0% (1/202) | 36.3 | 8375 | 0.13 |

## Exactitud por tipo de campo

| Tipo | Exactitud | Omitidos | Inventados |
|---|---|---|---|
| edad | 71% (5/7) | 1 | 0% (0/1) |
| fecha | 64% (7/11) | 0 | 0% (0/5) |
| hora | 50% (1/2) | 0 | 0% (0/5) |
| nombre | 100% (4/4) | 0 | 0% (0/10) |
| numero | 100% (2/2) | 0 | 0% (0/52) |
| opcion | 100% (5/5) | 0 | — |
| telefono | 67% (2/3) | 0 | 0% (0/6) |
| texto | 88% (21/24) | 1 | 1% (1/95) |
| texto_largo | 50% (6/12) | 2 | 0% (0/28) |

## Por documento

| HC | Dificultad | Exactitud | Inventados | Excluidos (dudosos) |
|---|---|---|---|---|
| hc-01 | media | 79% (15/19) | 0% (0/12) | 2 |
| hc-02 | facil | 100% (8/8) | 0% (0/25) | 0 |
| hc-03 | facil | 100% (9/9) | 0% (0/23) | 1 |
| hc-04 | media | 100% (5/5) | 0% (0/26) | 2 |
| hc-05 | facil | 80% (8/10) | 5% (1/21) | 2 |
| hc-06 | dificil | 50% (4/8) | 0% (0/22) | 3 |
| hc-07 | media | 100% (3/3) | 0% (0/26) | 4 |
| hc-08 | extrema | 0% (0/2) | 0% (0/25) | 6 |
| hc-09 | dificil | 17% (1/6) | 0% (0/22) | 5 |

## Calibración (¿sirve la confianza que declara el modelo?)

| Confianza declarada | Campos con respuesta | Mal leídos o inventados |
|---|---|---|
| alta | 43 | 9% (4/43) |
| media | 20 | 30% (6/20) |
| baja | 4 | 100% (4/4) |

## Estabilidad entre corridas

Campos con el mismo valor en las corridas 1 y 2: 86% (254/297).

## Errores (sin valores, para no exponer datos de pacientes)

| HC | Campo | Tipo | Resultado | Confianza | Similitud o cobertura |
|---|---|---|---|---|---|
| hc-01 | paciente.grado_instruccion | texto | incorrecto | media | 0.84 |
| hc-01 | paciente.telefono | telefono | incorrecto | media |  |
| hc-01 | atencion.fecha | fecha | incorrecto | alta |  |
| hc-01 | atencion.hora | hora | incorrecto | alta |  |
| hc-05 | paciente.domicilio | texto | incorrecto | baja | 0.77 |
| hc-05 | atencion.fecha | fecha | incorrecto | alta |  |
| hc-05 | atencion.servicio | texto | inventado | media |  |
| hc-06 | paciente.fecha_nacimiento | fecha | incorrecto | media |  |
| hc-06 | atencion.fecha | fecha | incorrecto | alta |  |
| hc-06 | clinica.relato | texto_largo | incorrecto | baja | 0.0 |
| hc-06 | clinica.diagnostico | texto_largo | incorrecto | media | 0.0 |
| hc-08 | paciente.edad | edad | incorrecto | media |  |
| hc-08 | clinica.relato | texto_largo | incorrecto | baja | 0.4 |
| hc-09 | paciente.edad | edad | omitido | alta |  |
| hc-09 | clinica.motivo_consulta | texto_largo | omitido | alta |  |
| hc-09 | clinica.tiempo_enfermedad | texto | omitido | alta |  |
| hc-09 | clinica.relato | texto_largo | omitido | baja |  |
| hc-09 | clinica.tratamiento | texto_largo | incorrecto | baja | 0.17 |
