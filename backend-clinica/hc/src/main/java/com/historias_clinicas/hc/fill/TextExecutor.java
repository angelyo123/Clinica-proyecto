package com.historias_clinicas.hc.fill;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.cognitive.TargetRegion;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

public class TextExecutor {

    public void execute(
            XWPFTableCell cell,
            FieldDefinition def,
            String valor
    ) {

        TargetRegion r = def.getTargetRegion();

        if (r == null || r.getMode() == null) {
            writeReplace(cell, valor);
            return;
        }

        switch (r.getMode()) {

            case "WHOLE_CELL" -> writeReplace(cell, valor);

            case "INLINE_AFTER_TEXT" -> append(cell, valor);

            case "BELOW_LABEL" -> writeBelow(cell, valor);

            default -> writeReplace(cell, valor);
        }
    }

    // ==============================
    // IMPLEMENTACIONES
    // ==============================

    private void writeReplace(XWPFTableCell cell, String texto) {

        XWPFParagraph p = getOrCreateParagraph(cell);
        clearRuns(p);

        XWPFRun run = p.createRun();
        run.setText(texto);
    }

    private void append(XWPFTableCell cell, String texto) {

        XWPFParagraph p = getOrCreateParagraph(cell);

        XWPFRun run = p.createRun();
        run.setText(" " + texto);
    }

    private void writeBelow(XWPFTableCell cell, String texto) {

        XWPFParagraph p = cell.addParagraph();

        XWPFRun run = p.createRun();
        run.setText(texto);
    }

    // ==============================
    // UTILIDADES
    // ==============================

    private XWPFParagraph getOrCreateParagraph(XWPFTableCell cell) {

        if (cell.getParagraphs().isEmpty()) {
            return cell.addParagraph();
        }
        return cell.getParagraphs().get(0);
    }

    private void clearRuns(XWPFParagraph p) {

        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }
    }
}