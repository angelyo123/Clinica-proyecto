package com.historias_clinicas.hc.servicios.word;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.dto.CampoValorConfirmado;
import com.historias_clinicas.hc.fill.MarkExecutor;
import com.historias_clinicas.hc.fill.TextExecutor;
import com.historias_clinicas.hc.visual.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlantillaFillService {


    private MarkExecutor  markExecutor;
    private TextExecutor textExecutor;

    public byte[] llenarWord(
            byte[] plantillaBytes,
            List<CampoValorConfirmado> valores,
            List<FieldDefinition> defs
    ) throws Exception {



        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantillaBytes)
        );

        Map<String, FieldDefinition> defsPorWriteId = defs.stream()
                .collect(Collectors.toMap(
                        FieldDefinition::getWriteStableId,
                        d -> d,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        VisualDocumentBuilder builder = new VisualDocumentBuilder();
        VisualDocument visual = builder.build(doc);

        StableIdGenerator generator = new StableIdGenerator();
        generator.generarIds(visual);

        // 🔥 AQUÍ VA
        Map<String, List<CampoValorConfirmado>> valoresPorId =
                valores.stream().collect(java.util.stream.Collectors.groupingBy(
                        CampoValorConfirmado::getStableId
                ));

        aplicarValores(doc, visual, valoresPorId, defsPorWriteId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }


    private void aplicarValores(
            XWPFDocument doc,
            VisualDocument visual,
            Map<String, List<CampoValorConfirmado>> valores,
            Map<String, FieldDefinition> defs)

    {
        System.out.println("Total valores: " + valores.size());

        for (VisualBlock block : visual.getBlocks()) {

            if (block instanceof TableBlock tb) {

                TableGrid grid = tb.getGrid();
                CellVisual[][] matrix = grid.getMatrix();
                XWPFTable tabla = doc.getTables().get(tb.getTableIndex());
                for (int r = 0; r < grid.getTotalRows(); r++) {
                    for (int c = 0; c < grid.getTotalCols(); c++) {
                        CellVisual cell = matrix[r][c];
                        if (cell == null) continue;
                        if (!cell.isMaster()) continue;
                        String stableId = cell.getStableId();
                        System.out.println("Celda actual: " + stableId);
                        if (!valores.containsKey(stableId)) {
                            System.out.println("❌ NO MATCH: " + stableId);
                            continue;
                        }
                        List<CampoValorConfirmado> campos = valores.get(stableId);
                        XWPFTableRow row = tabla.getRow(r);
                        XWPFTableCell realCell =
                                row.getCell(cell.getColStart());
                        System.out.println("🔥 MULTI MATCH: " + stableId + " -> " + campos.size());
                        ejecutarGrupo(realCell, campos, defs);
                    }
                }
            }
        }
    }



    private void ejecutarGrupo(
            XWPFTableCell cell,
            List<CampoValorConfirmado> campos,
            Map<String, FieldDefinition> defs
    ) {
        for (CampoValorConfirmado campo : campos) {

            FieldDefinition def = defs.get(campo.getStableId());

            if (def == null) {
                System.out.println("❌ Sin definición para " + campo.getStableId());
                continue;
            }

            String writeMode = def.getWriteMode();

            if ("REWRITE".equals(writeMode)) {

                System.out.println("🔁 REWRITE -> " + campo.getStableId());
                writeReplace(cell, def.getRewriteText());

            } else if ("MARK".equals(writeMode)) {

                if (isTruthy(campo.getValor())) {
                    System.out.println("☑️ MARK -> " + campo.getStableId());
                    markExecutor.execute(cell, def);
                }

            } else if ("TEXT".equals(writeMode)) {

                System.out.println("✍️ TEXT -> " + campo.getValor());
                textExecutor.execute(cell, def, campo.getValor());

            } else {

                System.out.println("⚠️ writeMode no soportado: " + writeMode);
            }
        }
    }

    private void writeReplace(XWPFTableCell cell, String texto) {

        if (cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }

        XWPFParagraph p = cell.getParagraphs().get(0);

        // limpiar contenido
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }

        XWPFRun run = p.createRun();
        run.setText(texto);
    }

    private boolean isTruthy(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("x") || v.equals("si");
    }

    private void escribirCeldaSeguro(XWPFTableCell cell, String texto) {

        if (cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }

        XWPFParagraph p = cell.getParagraphs().get(0);

        // ❌ NO BORRAR
        // cell.removeParagraph(0);

        // 👉 opcional: limpiar solo si quieres replace real
        p.getRuns().clear();

        XWPFRun run = p.createRun();
        run.setText(texto);
    }
}