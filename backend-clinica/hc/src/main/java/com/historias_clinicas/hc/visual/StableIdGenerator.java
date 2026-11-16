package com.historias_clinicas.hc.visual;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StableIdGenerator {

    public void generarIds(VisualDocument doc) {

        for (VisualBlock block : doc.getBlocks()) {

            if (!(block instanceof TableBlock tb)) continue;

            String blockHash = buildBlockHash(doc, tb);

            TableGrid grid = tb.getGrid();
            CellVisual[][] m = grid.getMatrix();

            for (int r = 0; r < grid.getTotalRows(); r++) {
                for (int c = 0; c < grid.getTotalCols(); c++) {

                    CellVisual cell = m[r][c];
                    if (cell == null || !cell.isMaster()) continue;

                    String geom = "R" + r + "C" + c +
                            "rs" + cell.getRowSpan() +
                            "cs" + cell.getColSpan();

                    String left = nearestLeftLabel(m, r, c);
                    String top  = nearestTopLabel(m, r, c);

                    String anchor = normalize(left) + "|" + normalize(top);

                    String anchorHash =
                            DigestUtils.sha1Hex(anchor).substring(0, 10);

                    String path = cell.getPath();

                    cell.setStableId(
                            "B" + blockHash.substring(0,8)
                                    + "/" + path
                                    + "/" + geom
                                    + "#" + anchorHash
                    );
                }
            }
        }
    }

    private String buildBlockHash(VisualDocument doc, TableBlock tb) {

        String title = "";
        String firstRow = "";

        // buscar párrafos antes
        for (VisualBlock b : doc.getBlocks()) {
            if (b instanceof ParagraphBlock p &&
                    p.getBodyIndex() < tb.getBodyIndex()) {
                title += p.getText() + "|";
            }
        }

        // primera fila no vacía
        CellVisual[][] m = tb.getGrid().getMatrix();
        for (CellVisual cell : m[0]) {
            if (cell != null && cell.isMaster() &&
                    !cell.getText().isBlank()) {
                firstRow += cell.getText() + "|";
            }
        }

        return DigestUtils.sha1Hex(title + firstRow);
    }

    private String nearestLeftLabel(CellVisual[][] m, int r, int c) {
        for (int cc = c - 1; cc >= 0; cc--) {
            CellVisual x = m[r][cc];
            if (x != null && x.isMaster() &&
                    x.getText() != null &&
                    !x.getText().isBlank()) {
                return x.getText();
            }
        }
        return "";
    }

    private String nearestTopLabel(CellVisual[][] m, int r, int c) {
        for (int rr = r - 1; rr >= 0; rr--) {
            CellVisual x = m[rr][c];
            if (x != null && x.isMaster() &&
                    x.getText() != null &&
                    !x.getText().isBlank()) {
                return x.getText();
            }
        }
        return "";
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}