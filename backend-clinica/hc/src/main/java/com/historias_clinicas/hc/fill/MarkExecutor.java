package com.historias_clinicas.hc.fill;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.cognitive.TargetRegion;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;


public class MarkExecutor {

    public void execute(XWPFTableCell cell, FieldDefinition def) {

        TargetRegion r = def.getTargetRegion();

        if (r == null || r.getMode() == null) {
            throw new RuntimeException("MARK sin targetRegion definido");
        }

        String mark = def.getWriteValue();
        if (mark == null) mark = "X";

        switch (r.getMode()) {

            case "WHOLE_CELL" -> writeWholeCell(cell, mark);

            case "BETWEEN_TEXTS" ->
                    markBetween(cell,
                            r.getAnchorText(),
                            r.getSecondAnchorText(),
                            mark);

            case "INSIDE_EXISTING_MARK" ->
                    markInside(cell, r.getAnchorText(), mark);

            default -> throw new RuntimeException("Modo no soportado: " + r.getMode());
        }
    }

    // ==============================
    // IMPLEMENTACIONES
    // ==============================

    private void writeWholeCell(XWPFTableCell cell, String mark) {

        XWPFParagraph p = getOrCreateParagraph(cell);
        clearRuns(p);

        XWPFRun run = p.createRun();
        run.setText(mark);
    }

    private void markBetween(XWPFTableCell cell, String left, String right, String mark) {

        String text = cell.getText();
        if (text == null) text = "";

        int i1 = text.indexOf(left);
        int i2 = text.indexOf(right);

        if (i1 == -1 || i2 == -1 || i2 <= i1) {
            throw new RuntimeException("BETWEEN_TEXTS inválido: no se encontraron anchors");
        }

        int start = i1 + left.length();
        String nuevo = text.substring(0, start)
                + " (" + mark + ") "
                + text.substring(i2);

        replaceWholeCell(cell, nuevo);
    }

    private void markInside(XWPFTableCell cell, String anchor, String mark) {

        String text = cell.getText();
        if (text == null) text = "";

        int pos = text.indexOf(anchor);

        if (pos == -1) {
            throw new RuntimeException("INSIDE_EXISTING_MARK: anchor no encontrado");
        }

        int open = text.indexOf("(", pos);
        int close = text.indexOf(")", open);

        if (open == -1 || close == -1) {
            throw new RuntimeException("INSIDE_EXISTING_MARK: no hay paréntesis");
        }

        String nuevo = text.substring(0, open + 1)
                + mark
                + text.substring(close);

        replaceWholeCell(cell, nuevo);
    }

    // ==============================
    // UTILIDADES
    // ==============================

    private void replaceWholeCell(XWPFTableCell cell, String texto) {

        XWPFParagraph p = getOrCreateParagraph(cell);
        clearRuns(p);

        XWPFRun run = p.createRun();
        run.setText(texto);
    }

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