package com.historias_clinicas.hc.cognitive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.dto.CognitiveCell;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CognitivePromptBuilder {

    private final ObjectMapper mapper = new ObjectMapper();

    public String buildPrompt(List<CognitiveCell> cells) {

        String json;

        try {
            json = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(cells);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error serializando CognitiveCell",
                    e
            );
        }

        return """
Eres un sistema cognitivo avanzado que interpreta documentos clínicos.

NO estás detectando celdas.
Estás entendiendo cómo un humano interactuaría con el documento.

============================================================
🧠 CONCEPTO FUNDAMENTAL
============================================================

Un campo NO es una celda.

Un campo es una INSTRUCCIÓN DE INTERACCIÓN que define:

- dónde se entiende el dato
- dónde se escribe
- cómo se escribe
- qué parte se preserva
- qué tipo de superficie representa

============================================================
🎯 TAREA
============================================================

Para cada campo debes devolver:

1. stableId → dónde se entiende el campo
2. writeStableId → dónde se interviene
3. interactionType → tipo de interacción
4. preservationMode → cómo tratar el contenido existente
5. targetSemantics → tipo de superficie
6. targetRegion → (si aplica) zona específica dentro de la celda

============================================================
🧠 TIPOS DE INTERACCIÓN
============================================================

WRITE_TEXT → escribir valor en celda vacía
APPEND_INLINE → agregar al texto existente (ej: "Nombre: Juan")
WRITE_BELOW → escribir debajo del label
TOGGLE_MARK → marcar o desmarcar (checkbox textual)
SELECT_OPTION → elegir opción dentro de múltiples
WRITE_MULTILINE → escribir texto largo clínico

============================================================
🧠 PRESERVACIÓN
============================================================

PRESERVE_EXISTING → no borrar contenido
REPLACE_CONTENT → reemplazar completamente
MERGE_WITH_EXISTING → mezclar con contenido existente

============================================================
🧠 SEMÁNTICA
============================================================

SLOT → celda vacía para valor
LABEL_WITH_SLOT → label con espacio implícito
OPTION_CONTROL → opciones tipo checkbox/radio
WHOLE_CELL → la celda es el control
FREE_TEXT_AREA → texto clínico largo

============================================================
🧠 EJECUCIÓN ESTRICTA
============================================================

El sistema que ejecuta tu salida:

- NO interpreta intención
- NO detecta patrones por sí mismo
- NO usa heurísticas
- NO decide dinámicamente cómo intervenir

Puede ejecutar texto, marcas o reescrituras completas
solo a partir del contrato que tú definas.

Los símbolos visibles del documento pueden existir,
pero el sistema no debe depender de ellos para decidir.
Esa decisión te corresponde a ti.


Por lo tanto:

- Debes definir writeMode (TEXT | MARK | REWRITE)
- Debes definir writeValue SOLO cuando writeMode sea MARK
- Debes definir rewriteText SOLO cuando writeMode sea REWRITE
- Debes definir targetRegion con precisión cuando la ejecución sea localizada (TEXT o MARK)
- NO es necesario definir targetRegion cuando writeMode sea REWRITE

Si no defines correctamente estos elementos,
el campo no podrá ejecutarse.

============================================================
🧠 DECISIÓN ESTRUCTURAL DE ESCRITURA
============================================================

Debes decidir writeMode según la naturaleza REAL de la intervención,
no según palabras específicas del documento ni del dominio.

Usa estas reglas universales:

1. writeMode = TEXT
   cuando el valor debe escribirse como contenido textual nuevo
   sin reconstruir toda la superficie existente.

2. writeMode = MARK
   cuando la intervención consiste en ubicar una marca puntual
   en una región específica ya existente dentro de una superficie,
   sin reescribir completamente el contenido visual.

3. writeMode = REWRITE
   cuando para expresar correctamente el resultado final
   es necesario reconstruir toda la superficie textual
   o reescribir completamente el contenido visible.

============================================================
🧠 REGLA ESTRUCTURAL DE SUPERFICIE (CRÍTICO)
============================================================

Debes analizar la estructura de la superficie donde ocurre la interacción.

Una superficie puede ser:

1. SEGMENTADA
   - múltiples regiones independientes
   - cada opción ocupa su propio espacio físico
   - cada opción puede ser intervenida sin afectar las demás

2. NO SEGMENTADA
   - múltiples elementos comparten una misma superficie continua
   - no existe separación física real entre opciones
   - intervenir una parte implica reconstruir el todo

------------------------------------------------------------

Usa MARK cuando la superficie sea SEGMENTADA:

- cada opción tiene su propio espacio independiente
- puedes intervenir una región sin modificar el resto
- la acción es localizada

------------------------------------------------------------

Usa REWRITE cuando la superficie sea NO SEGMENTADA:

- múltiples opciones conviven en una misma superficie continua
- no es posible intervenir una opción sin afectar el resto
- el resultado correcto requiere reconstruir toda la superficie

------------------------------------------------------------
============================================================
🧠 TARGET REGION EN SUPERFICIES SEGMENTADAS
============================================================

Cuando la superficie sea segmentada:

- debes usar targetRegion
- si la región completa es el punto de intervención:
  → targetRegion.mode = WHOLE_CELL
  
⚠️ Regla absoluta:

Si puedes intervenir una región sin modificar el resto → MARK

Si modificar una región implica rehacer el contenido completo → REWRITE

============================================================
🧠 REWRITE (CRÍTICO)
============================================================

Si writeMode = REWRITE:

- debes devolver rewriteText
- rewriteText debe contener el contenido final completo
  tal como debe quedar visible en la superficie
- el sistema reemplazará completamente esa superficie
- no debes delegar al sistema ninguna reconstrucción adicional

Si writeMode ≠ REWRITE:
- NO debes incluir rewriteText

============================================================
📦 SALIDA (ACTUALIZADA)
============================================================

{
  "fields": [
    {
      "stableId": "...",
      "writeStableId": "...",
      "description": "...",
      "dataType": "texto|numero|fecha|booleano",
      "interactionType": "...",
      "preservationMode": "...",
      "targetSemantics": "...",
      "writeMode": "TEXT|MARK|REWRITE",
      "writeValue": "...",
      "rewriteText": "...",
      "targetRegion": {
        "mode": "...",
        "anchorText": "...",
        "secondAnchorText": "...",
        "relativeRow": 0,
        "relativeCol": 0
      }
    }
  ]
}

============================================================
📄 DOCUMENTO
============================================================
""" + json;
    }
}