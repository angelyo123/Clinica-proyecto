package com.historias_clinicas.hc.servicios.word;

import com.historias_clinicas.hc.dto.CognitiveCell;
import com.historias_clinicas.hc.visual.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CognitiveSerializer {

    public List<CognitiveCell> serialize(VisualDocument doc) {

        List<CognitiveCell> result = new ArrayList<>();

        for (VisualBlock block : doc.getBlocks()) {

            if (block instanceof TableBlock tb) {

                TableGrid grid = tb.getGrid();
                CellVisual[][] m = grid.getMatrix();

                for (int r = 0; r < grid.getTotalRows(); r++) {
                    for (int c = 0; c < grid.getTotalCols(); c++) {

                        CellVisual cell = m[r][c];

                        if (cell == null || !cell.isMaster()) continue;

                        CognitiveCell dto = new CognitiveCell();
                        dto.setStableId(cell.getStableId());
                        dto.setText(cell.getText());
                        dto.setLeftContext(findLeft(m, r, c));
                        dto.setTopContext(findTop(m, r, c));
                        dto.setSectionTitle(findSectionTitle(doc, tb));
                        dto.setBlockType("table");
                        dto.setBodyIndex(tb.getBodyIndex());
                        dto.setRow(r);
                        dto.setCol(c);
                        dto.setDepth(0);

                        result.add(dto);
                    }
                }
            }
        }

        return result;
    }

    private String findLeft(CellVisual[][] m, int r, int c) {

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

    private String findTop(CellVisual[][] m, int r, int c) {

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

    private String findSectionTitle(VisualDocument doc, TableBlock tb) {

        String lastTitle = "";

        for (VisualBlock b : doc.getBlocks()) {

            if (b instanceof ParagraphBlock p &&
                    p.getBodyIndex() < tb.getBodyIndex()) {

                if (!p.getText().isBlank()) {
                    lastTitle = p.getText();
                }
            }
        }

        return lastTitle;
    }
}