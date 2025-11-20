package com.historias_clinicas.hc.servicios;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.ia.DeepSeekClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaExtractorService {

    private final DeepSeekClient deepSeek;

    // ==========================================================
    // 1. CONVERTIR WORD LLENO → WORD PLANTILLA VACÍA
    // ==========================================================
    public byte[] generarPlantillaDesdeWord(byte[] wordLleno) {

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(wordLleno))) {

            // 1️⃣ Extraer texto real del Word
            String texto = extraerTexto(doc);

            // 2️⃣ IA detecta campos y valores
            Map<String, Object> info = analizarConIA(texto);

            Map<String, Object> valores = (Map<String, Object>) info.get("json_valores");
            if (valores == null) throw new RuntimeException("La IA no devolvió json_valores");

            // 3️⃣ Borrar valores en el Word (POI)
            limpiarValores(doc, valores);

            // 4️⃣ Devolver Word convertido en plantilla
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando plantilla vacía: " + e.getMessage(), e);
        }
    }


    // ==========================================================
    // 2. EXTRAER TODO EL TEXTO DEL WORD
    // ==========================================================
    private String extraerTexto(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();

        for (XWPFParagraph p : doc.getParagraphs())
            sb.append(p.getText()).append("\n");

        for (XWPFTable t : doc.getTables())
            for (XWPFTableRow r : t.getRows())
                for (XWPFTableCell c : r.getTableCells())
                    for (XWPFParagraph p : c.getParagraphs())
                        sb.append(p.getText()).append("\n");

        return sb.toString();
    }


    // ==========================================================
    // 3. ANALIZAR CON IA → obtener valores exactos del Word
    // ==========================================================
    private Map<String, Object> analizarConIA(String texto) throws Exception {

        String prompt = """
        Eres un experto en HISTORIAS CLÍNICAS.

        Lee el texto siguiente y devuelve SOLO JSON:

        {
          "json_valores": {
             "campo": "valor detectado EXACTO",
             ...
          }
        }

        NO EXPLIQUES NADA.
        NO USES MARKDOWN.
        NO AGREGUES TEXTO FUERA DEL JSON.
        SIN backticks

        TEXTO:
        """ + texto;

        String resp = deepSeek.completar(prompt);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(resp, Map.class);
    }


    // ==========================================================
    // 4. BORRAR VALORES Y DEJAR HUECOS (POI)
    // ==========================================================
    private void limpiarValores(XWPFDocument doc, Map<String, Object> valores) {

        for (XWPFParagraph p : doc.getParagraphs()) {
            limpiarParrafo(p, valores);
        }

        for (XWPFTable tabla : doc.getTables()) {
            for (XWPFTableRow fila : tabla.getRows()) {
                for (XWPFTableCell celda : fila.getTableCells()) {
                    for (XWPFParagraph p : celda.getParagraphs()) {
                        limpiarParrafo(p, valores);
                    }
                }
            }
        }
    }


    // ==========================================================
    // 5. LIMPIAR VALORES DE UN PÁRRAFO Y PONER HUECOS
    // ==========================================================
    private void limpiarParrafo(XWPFParagraph p, Map<String, Object> valores) {

        String original = p.getText();

        for (String clave : valores.keySet()) {
            Object v = valores.get(clave);
            if (v == null) continue;

            String valor = v.toString().trim();
            if (valor.isBlank()) continue;

            if (original.contains(valor)) {

                String nuevoTexto = original.replace(valor, "__________");

                // eliminar runs
                List<XWPFRun> runs = p.getRuns();
                for (int i = runs.size() - 1; i >= 0; i--) {
                    p.removeRun(i);
                }

                XWPFRun run = p.createRun();
                run.setText(nuevoTexto);
                run.setUnderline(UnderlinePatterns.SINGLE);   // opcional
            }
        }
    }
}
