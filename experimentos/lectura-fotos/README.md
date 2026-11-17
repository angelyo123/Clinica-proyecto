# Experimento de lectura de fotos de HC (tarea 04)

Script independiente: no forma parte de los servicios. Lee las fotos de `tareas/03-dataset-de-prueba/dataset/imagenes/` con un modelo de visión de DeepSeek y compara lo leído contra `dataset/respuestas/`.

## Requisitos

- Python 3.10 o superior con `pillow` (`pip install pillow`).
- Una clave de DeepSeek en la variable `DEEPSEEK_API_KEY`, o en un archivo indicado por `DEEPSEEK_API_KEY_FILE`. La clave del repositorio ya no sirve.

## Uso

```bash
python -I leer_fotos.py correr --variante v1 --corrida 1
```

```bash
python -I leer_fotos.py evaluar --variante v1 --corridas 1 2
```

- `--solo hc-02 hc-05` limita la corrida a algunas fotos.
- `--modelo` permite probar otro modelo. Ojo: `deepseek-v4-pro` descarta la imagen sin avisar.
- Las respuestas crudas quedan en `salidas/`, que no se sube a git porque contiene datos leídos de las fotos.
- El resumen sin datos personales va a `tareas/04-experimento-lectura/implementacion/evidencias/`.

## Agregar fotos

1. Copia la foto como `dataset/imagenes/hc-NN.jpg`.
2. Crea `dataset/respuestas/hc-NN.json` con los campos de `dataset/esquema.json` que tiene el formato (ver `dataset/fuentes.md`).
3. Vuelve a correr y a evaluar.
