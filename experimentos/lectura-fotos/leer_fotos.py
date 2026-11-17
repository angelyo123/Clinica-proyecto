"""Experimento E1 (tarea 04): un modelo con visión lee fotos de HC llenadas a mano y devuelve
los campos de un esquema en JSON. Después se compara contra las respuestas transcritas.

Uso:
  python -I leer_fotos.py correr  --variante v1 --corrida 1 [--modelo deepseek-flash] [--solo hc-01]
  python -I leer_fotos.py evaluar --variante v1 --corridas 1 2

Variantes:
  v1  la foto completa
  v2  la foto completa + sus mitades superior e inferior ampliadas (más píxeles para la letra)

La clave de la API se lee de la variable DEEPSEEK_API_KEY o del archivo indicado en DEEPSEEK_API_KEY_FILE.
Nunca se imprime ni se guarda en las salidas.

Las salidas crudas (con datos de pacientes) van a ./salidas/, que está en .gitignore.
El resumen sin datos personales va a la carpeta de evidencias de la tarea 04.
"""
import argparse, base64, datetime, difflib, io, json, os, re, sys, time, unicodedata, urllib.request
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
DATASET = RAIZ / "tareas" / "03-dataset-de-prueba" / "dataset"
SALIDAS = Path(__file__).resolve().parent / "salidas"
EVIDENCIAS = RAIZ / "tareas" / "04-experimento-lectura" / "implementacion" / "evidencias"
API = "https://api.deepseek.com"


# ----------------------------------------------------------------------------- llamada al modelo

def clave_api():
    if os.environ.get("DEEPSEEK_API_KEY"):
        return os.environ["DEEPSEEK_API_KEY"].strip()
    archivo = os.environ.get("DEEPSEEK_API_KEY_FILE")
    if archivo and Path(archivo).exists():
        return Path(archivo).read_text().strip()
    sys.exit("Falta DEEPSEEK_API_KEY o DEEPSEEK_API_KEY_FILE")


def pedir(ruta, clave, cuerpo=None, timeout=300):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(API + ruta, data=datos,
                                 headers={"Authorization": f"Bearer {clave}", "Content-Type": "application/json"})
    return json.load(urllib.request.urlopen(req, timeout=timeout))


def saldo(clave):
    try:
        b = pedir("/user/balance", clave)
        return float(b["balance_infos"][0]["total_balance"])
    except Exception:
        return None


def jpg_b64(img):
    buf = io.BytesIO()
    img.convert("RGB").save(buf, format="JPEG", quality=92)
    return base64.b64encode(buf.getvalue()).decode()


def imagenes_para(ruta, variante):
    img = Image.open(ruta)
    partes = [("completa", img)]
    if variante == "v2":
        w, h = img.size
        # Mitades con 8% de solapamiento, ampliadas x2 (el servicio las reduce a ~1300 px por lado)
        for nombre, (y0, y1) in [("mitad superior", (0, 0.54)), ("mitad inferior", (0.46, 1.0))]:
            r = img.crop((0, int(y0 * h), w, int(y1 * h)))
            r = r.resize((r.width * 2, r.height * 2), Image.LANCZOS)
            partes.append((nombre, r))
    return partes


def prompt(esquema, variante):
    lineas = []
    for c in esquema["campos"]:
        extra = f" (opciones: {', '.join(c['opciones'])})" if c.get("opciones") else ""
        lineas.append(f'- "{c["id"]}": {c["etiqueta"]}; tipo {c["tipo"]}{extra}')
    vistas = ("Recibirás la foto completa y, después, sus mitades superior e inferior ampliadas para leer mejor la letra. "
              "Son la misma página: no dupliques datos.\n" if variante == "v2" else "")
    return (
        "Eres un transcriptor de historias clínicas en papel llenadas a mano. "
        "Lee la imagen y devuelve SOLO un objeto JSON.\n" + vistas +
        "\nCampos a extraer:\n" + "\n".join(lineas) +
        "\n\nReglas:\n"
        "1. Copia lo que está ESCRITO en el documento. No corrijas, no completes y no deduzcas datos que no estén escritos.\n"
        "2. Si un campo no existe en el formato, está en blanco, tachado, difuminado o no se puede leer, usa null.\n"
        "3. No copies las etiquetas impresas del formulario ni las notas añadidas por otra persona en los márgenes.\n"
        "4. Fechas en DD/MM/AAAA. Horas en HH:MM de 24 horas. Sexo como M o F solo si está escrito o marcado.\n"
        "5. Edad: el número con su unidad si no son años (por ejemplo \"15 meses\"). Peso en kg y talla en cm, solo el número.\n"
        "6. Textos largos (relato, antecedentes, diagnóstico, tratamiento): transcribe completo lo que se lea.\n"
        "7. Para cada campo indica tu confianza en la lectura: \"alta\", \"media\" o \"baja\".\n"
        "\nFormato de respuesta:\n"
        '{"campos": {"<id>": {"valor": <texto o null>, "confianza": "alta|media|baja"}, ...}}\n'
        "Incluye todos los ids de la lista, aunque su valor sea null."
    )


def leer_una(ruta, esquema, variante, modelo, clave):
    contenido = [{"type": "text", "text": prompt(esquema, variante)}]
    for nombre, img in imagenes_para(ruta, variante):
        contenido.append({"type": "text", "text": f"Imagen: {nombre}"})
        contenido.append({"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{jpg_b64(img)}", "detail": "high"}})
    cuerpo = {"model": modelo, "messages": [{"role": "user", "content": contenido}],
              "response_format": {"type": "json_object"}, "temperature": 0}
    for intento in range(3):
        t = time.time()
        try:
            r = pedir("/chat/completions", clave, cuerpo)
            texto = r["choices"][0]["message"]["content"]
            return {"segundos": round(time.time() - t, 1), "uso": r.get("usage"), "texto": texto, "json": parsear(texto)}
        except urllib.error.HTTPError as e:
            err = e.read()[:300].decode("utf-8", "replace")
            if intento == 2:
                return {"error": f"HTTP {e.code}: {err}"}
        except Exception as e:  # red, JSON inválido, etc.
            if intento == 2:
                return {"error": f"{type(e).__name__}: {str(e)[:200]}"}
        time.sleep(3 * (intento + 1))


def parsear(texto):
    t = texto.strip()
    t = re.sub(r"^```(?:json)?|```$", "", t, flags=re.M).strip()
    return json.loads(t)


# ----------------------------------------------------------------------------- comparación

MESES = {"ene": 1, "feb": 2, "mar": 3, "abr": 4, "may": 5, "jun": 6, "jul": 7, "ago": 8, "sep": 9, "set": 9,
         "oct": 10, "nov": 11, "dic": 12}


def norm(s):
    s = unicodedata.normalize("NFKD", str(s)).encode("ascii", "ignore").decode().lower()
    s = re.sub(r"[^a-z0-9]+", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def similitud(a, b):
    return difflib.SequenceMatcher(None, norm(a), norm(b)).ratio()


def fecha(s):
    t = norm(s)
    m = re.search(r"\b(\d{1,2}) (?:de )?([a-z]{3,})[a-z]* (?:de )?(\d{2,4})\b", t)
    if m and m.group(2)[:3] in MESES:
        d, mes, a = int(m.group(1)), MESES[m.group(2)[:3]], int(m.group(3))
    else:
        m = re.search(r"\b(\d{1,2}) (\d{1,2}) (\d{2,4})\b", t)
        if not m:
            return None
        d, mes, a = int(m.group(1)), int(m.group(2)), int(m.group(3))
    if a < 100:
        a += 2000 if a <= 30 else 1900
    return (d, mes, a)


def hora(s):
    t = str(s).lower()
    m = re.search(r"(\d{1,2})\s*[:.h]\s*(\d{2})", t) or re.search(r"(\d{1,2})()\s*(?:am|pm|a\.m|p\.m)", t)
    if not m:
        return None
    h, mi = int(m.group(1)), int(m.group(2) or 0)
    if re.search(r"p\.?\s?m", t) and h < 12:
        h += 12
    if re.search(r"a\.?\s?m", t) and h == 12:
        h = 0
    return h * 60 + mi


def numero(s):
    m = re.search(r"\d+(?:[.,]\d+)?", str(s))
    return float(m.group().replace(",", ".")) if m else None


def edad(s):
    t = norm(s)
    n = numero(t)
    unidad = "mes" if "mes" in t else ("dia" if "dia" in t else "ano")
    return (n, unidad)


def sexo(s):
    t = norm(s)
    if t in ("m", "masculino", "hombre", "varon", "h"):
        return "M"
    if t in ("f", "femenino", "mujer"):
        return "F"
    return t


def cobertura_claves(claves, texto):
    """Proporción de claves que aparecen en el texto (exactas o con similitud >= 0.85)."""
    t = norm(texto)
    palabras = t.split()
    encontradas = 0
    for c in claves:
        c = norm(c)
        if c in t:
            encontradas += 1
            continue
        n = len(c.split())
        mejor = max((difflib.SequenceMatcher(None, c, " ".join(palabras[i:i + n])).ratio()
                     for i in range(max(1, len(palabras) - n + 1))), default=0)
        encontradas += mejor >= 0.85
    return encontradas / len(claves)


def comparar(tipo, esperado, obtenido):
    """Devuelve (correcto: bool, detalle: float|str)."""
    if tipo == "texto_largo":
        cob = cobertura_claves(esperado["claves"], obtenido)
        return cob >= 0.6, round(cob, 2)
    v = esperado["valor"]
    if tipo == "fecha":
        return fecha(v) == fecha(obtenido), ""
    if tipo == "hora":
        return hora(v) == hora(obtenido), ""
    if tipo == "edad":
        return edad(v) == edad(obtenido), ""
    if tipo == "numero":
        a, b = numero(v), numero(obtenido)
        return a is not None and b is not None and abs(a - b) <= 0.01 * max(abs(a), 1), ""
    if tipo == "telefono":
        return re.sub(r"\D", "", v) == re.sub(r"\D", "", str(obtenido)), ""
    if tipo == "opcion":
        return sexo(v) == sexo(obtenido), ""
    if tipo == "nombre":
        a, b = " ".join(sorted(norm(v).split())), " ".join(sorted(norm(obtenido).split()))
        s = difflib.SequenceMatcher(None, a, b).ratio()
        return s >= 0.85, round(s, 2)
    s = similitud(v, obtenido)
    return s >= 0.85, round(s, 2)


def vacio(v):
    return v is None or (isinstance(v, str) and norm(v) in ("", "null", "none", "n a", "na", "no aplica", "ninguno registrado"))


def evaluar_hc(esquema, resp, salida):
    campos_pred = (salida.get("json") or {}).get("campos", {}) if "error" not in salida else {}
    filas = []
    for c in esquema["campos"]:
        cid, tipo = c["id"], c["tipo"]
        esperado = resp["campos"].get(cid, {"valor": None})
        pred = campos_pred.get(cid) or {}
        valor = pred.get("valor") if isinstance(pred, dict) else pred
        if isinstance(valor, (list, dict)):
            valor = json.dumps(valor, ensure_ascii=False)
        conf = (pred.get("confianza") if isinstance(pred, dict) else None) or "?"
        tiene_ref = "claves" in esperado or esperado.get("valor") is not None
        if esperado.get("dudoso"):
            resultado, detalle = "excluido", ""
        elif not tiene_ref:
            resultado, detalle = ("vacio_ok", "") if vacio(valor) else ("inventado", "")
        elif vacio(valor):
            resultado, detalle = "omitido", ""
        else:
            ok, detalle = comparar(tipo, esperado, valor)
            resultado = "correcto" if ok else "incorrecto"
        filas.append({"campo": cid, "tipo": tipo, "resultado": resultado, "detalle": detalle,
                      "confianza": conf, "esperado": esperado.get("valor", esperado.get("claves")), "obtenido": valor})
    return filas


# ----------------------------------------------------------------------------- comandos

def cmd_correr(a):
    esquema = json.loads((DATASET / "esquema.json").read_text(encoding="utf-8"))
    clave = clave_api()
    carpeta = SALIDAS / f"{a.modelo}-{a.variante}-corrida{a.corrida}"
    carpeta.mkdir(parents=True, exist_ok=True)
    s0 = saldo(clave)
    for resp_path in sorted((DATASET / "respuestas").glob("hc-*.json")):
        hc = resp_path.stem
        if a.solo and hc not in a.solo:
            continue
        r = leer_una(DATASET / "imagenes" / f"{hc}.jpg", esquema, a.variante, a.modelo, clave)
        (carpeta / f"{hc}.json").write_text(json.dumps(r, ensure_ascii=False, indent=1), encoding="utf-8")
        estado = r.get("error") or f"{r['segundos']}s, tokens {r['uso'].get('prompt_tokens')}+{r['uso'].get('completion_tokens')}"
        print(f"{hc}: {estado}", flush=True)
    s1 = saldo(clave)
    meta = {"modelo": a.modelo, "variante": a.variante, "corrida": a.corrida,
            "fecha": datetime.datetime.now().isoformat(timespec="seconds"),
            "costo_usd": round(s0 - s1, 4) if s0 is not None and s1 is not None else None}
    (carpeta / "_meta.json").write_text(json.dumps(meta, indent=1), encoding="utf-8")
    print("costo de la corrida (USD, por diferencia de saldo):", meta["costo_usd"])


def cmd_evaluar(a):
    esquema = json.loads((DATASET / "esquema.json").read_text(encoding="utf-8"))
    respuestas = {p.stem: json.loads(p.read_text(encoding="utf-8")) for p in sorted((DATASET / "respuestas").glob("hc-*.json"))}
    corridas = {}
    for n in a.corridas:
        carpeta = SALIDAS / f"{a.modelo}-{a.variante}-corrida{n}"
        filas = []
        tiempos, tokens = [], []
        for hc, resp in respuestas.items():
            salida = json.loads((carpeta / f"{hc}.json").read_text(encoding="utf-8"))
            if "error" not in salida:
                tiempos.append(salida["segundos"])
                tokens.append(salida["uso"].get("total_tokens", 0))
            for f in evaluar_hc(esquema, resp, salida):
                f["hc"] = hc
                f["dificultad"] = resp.get("dificultad")
                filas.append(f)
        meta = json.loads((carpeta / "_meta.json").read_text()) if (carpeta / "_meta.json").exists() else {}
        corridas[n] = {"filas": filas, "tiempos": tiempos, "tokens": tokens, "meta": meta}
        (carpeta / "_evaluacion.json").write_text(json.dumps(filas, ensure_ascii=False, indent=1), encoding="utf-8")

    md = informe(a, corridas, esquema)
    EVIDENCIAS.mkdir(parents=True, exist_ok=True)
    destino = EVIDENCIAS / f"resultados-{a.modelo}-{a.variante}.md"
    destino.write_text(md, encoding="utf-8")
    print(md)
    print(f"\n(guardado en {destino.relative_to(RAIZ)})")


def tasas(filas):
    con_ref = [f for f in filas if f["resultado"] in ("correcto", "incorrecto", "omitido")]
    sin_ref = [f for f in filas if f["resultado"] in ("vacio_ok", "inventado")]
    ok = sum(f["resultado"] == "correcto" for f in con_ref)
    inv = sum(f["resultado"] == "inventado" for f in sin_ref)
    return ok, len(con_ref), inv, len(sin_ref)


def pct(a, b):
    return f"{100 * a / b:.0f}% ({a}/{b})" if b else "—"


def informe(a, corridas, esquema):
    n0 = a.corridas[0]
    filas = corridas[n0]["filas"]
    lin = [f"# Resultados: {a.modelo}, variante {a.variante}", "",
           f"Corridas: {', '.join(map(str, a.corridas))}. Las tablas por tipo y por documento son de la corrida {n0}.", "",
           "## Resumen por corrida", "",
           "| Corrida | Exactitud (campos con respuesta) | Inventados (campos vacíos que la IA llenó) | Seg. promedio por HC | Tokens promedio por HC | Costo de la corrida (USD) |",
           "|---|---|---|---|---|---|"]
    for n, c in corridas.items():
        ok, tot, inv, totv = tasas(c["filas"])
        seg = sum(c["tiempos"]) / len(c["tiempos"]) if c["tiempos"] else 0
        tok = sum(c["tokens"]) / len(c["tokens"]) if c["tokens"] else 0
        lin.append(f"| {n} | {pct(ok, tot)} | {pct(inv, totv)} | {seg:.1f} | {tok:.0f} | {c['meta'].get('costo_usd', '—')} |")

    lin += ["", "## Exactitud por tipo de campo", "", "| Tipo | Exactitud | Omitidos | Inventados |", "|---|---|---|---|"]
    for tipo in sorted({c["tipo"] for c in esquema["campos"]}):
        ft = [f for f in filas if f["tipo"] == tipo]
        ok, tot, inv, totv = tasas(ft)
        om = sum(f["resultado"] == "omitido" for f in ft)
        if tot or inv:
            lin.append(f"| {tipo} | {pct(ok, tot)} | {om} | {pct(inv, totv)} |")

    lin += ["", "## Por documento", "", "| HC | Dificultad | Exactitud | Inventados | Excluidos (dudosos) |", "|---|---|---|---|---|"]
    for hc in sorted({f["hc"] for f in filas}):
        fh = [f for f in filas if f["hc"] == hc]
        ok, tot, inv, totv = tasas(fh)
        exc = sum(f["resultado"] == "excluido" for f in fh)
        lin.append(f"| {hc} | {fh[0]['dificultad']} | {pct(ok, tot)} | {pct(inv, totv)} | {exc} |")

    lin += ["", "## Calibración (¿sirve la confianza que declara el modelo?)", "",
            "| Confianza declarada | Campos con respuesta | Mal leídos o inventados |", "|---|---|---|"]
    for conf in ("alta", "media", "baja"):
        fc = [f for f in filas if f["confianza"] == conf and f["resultado"] in ("correcto", "incorrecto", "inventado")]
        malos = sum(f["resultado"] in ("incorrecto", "inventado") for f in fc)
        lin.append(f"| {conf} | {len(fc)} | {pct(malos, len(fc))} |")

    if len(corridas) > 1:
        a1, a2 = (corridas[n]["filas"] for n in a.corridas[:2])
        iguales = sum(norm(x["obtenido"] or "") == norm(y["obtenido"] or "") for x, y in zip(a1, a2))
        lin += ["", "## Estabilidad entre corridas", "",
                f"Campos con el mismo valor en las corridas {a.corridas[0]} y {a.corridas[1]}: {pct(iguales, len(a1))}."]

    lin += ["", "## Errores (sin valores, para no exponer datos de pacientes)", "",
            "| HC | Campo | Tipo | Resultado | Confianza | Similitud o cobertura |", "|---|---|---|---|---|---|"]
    for f in filas:
        if f["resultado"] in ("incorrecto", "omitido", "inventado"):
            lin.append(f"| {f['hc']} | {f['campo']} | {f['tipo']} | {f['resultado']} | {f['confianza']} | {f['detalle']} |")
    return "\n".join(lin) + "\n"


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("correr")
    c.add_argument("--variante", choices=["v1", "v2"], default="v1")
    c.add_argument("--corrida", type=int, default=1)
    c.add_argument("--modelo", default="deepseek-flash")
    c.add_argument("--solo", nargs="*")
    e = sub.add_parser("evaluar")
    e.add_argument("--variante", choices=["v1", "v2"], default="v1")
    e.add_argument("--corridas", type=int, nargs="+", default=[1])
    e.add_argument("--modelo", default="deepseek-flash")
    a = p.parse_args()
    {"correr": cmd_correr, "evaluar": cmd_evaluar}[a.cmd](a)


if __name__ == "__main__":
    main()
