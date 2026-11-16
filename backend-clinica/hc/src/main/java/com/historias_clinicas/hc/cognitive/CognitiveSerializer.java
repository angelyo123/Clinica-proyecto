package com.historias_clinicas.hc.cognitive;

import com.historias_clinicas.hc.dto.CognitiveCell;
import com.historias_clinicas.hc.visual.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                        dto.setRowSpan(cell.getRowSpan());
                        dto.setColSpan(cell.getColSpan());
                        dto.setDepth(tb.getDepth());
                        dto.setRightContext(findRight(m, r, c));
                        dto.setBottomContext(findBottom(m, r, c));
                        dto.setEmpty(cell.isEmpty());
                        result.add(dto);
                    }
                }
            }
        }

        return result;
    }

    private String findRight(CellVisual[][] m, int r, int c) {

        for (int cc = c + 1; cc < m[r].length; cc++) {

            CellVisual x = m[r][cc];

            if (x == null) continue;

            CellVisual master = x.isMaster() ? x : x.getMaster();

            if (master == null) master = x;

            String text = master.getText();

            if (text != null && !text.isBlank()) {
                return normalize(text);
            }
        }

        return "";
    }

    private String findBottom(CellVisual[][] m, int r, int c) {

        for (int rr = r + 1; rr < m.length; rr++) {

            CellVisual x = m[rr][c];

            if (x == null) continue;

            CellVisual master = x.isMaster() ? x : x.getMaster();

            if (master == null) master = x;

            String text = master.getText();

            if (text != null && !text.isBlank()) {
                return normalize(text);
            }
        }

        return "";
    }

    private int groupsIndex(int row, Map<Integer,String> groups){

        int index = 0;

        for(Integer r : groups.keySet().stream().sorted().toList()){

            if(r == row) return index;

            index++;
        }

        return -1;
    }

    private String findLeft(CellVisual[][] m, int r, int c) {

        for (int cc = c - 1; cc >= 0; cc--) {

            CellVisual x = m[r][cc];

            if (x == null) continue;

            CellVisual master = x.isMaster() ? x : x.getMaster();

            if (master == null) master = x;

            String text = master.getText();

            if (text != null && !text.isBlank()) {
                return normalize(text);
            }
        }

        return "";
    }

    private String findTop(CellVisual[][] m, int r, int c) {

        for (int rr = r - 1; rr >= 0; rr--) {

            CellVisual x = m[rr][c];

            if (x == null) continue;

            CellVisual master = x.isMaster() ? x : x.getMaster();

            if (master == null) master = x;

            String text = master.getText();

            if (text != null && !text.isBlank()) {
                return normalize(text);
            }
        }

        return "";
    }

    private String findSectionTitle(VisualDocument doc, TableBlock tb) {

        String lastTitle = "";

        for (VisualBlock b : doc.getBlocks()) {

            if (!(b instanceof ParagraphBlock p)) continue;

            if (p.getBodyIndex() >= tb.getBodyIndex())
                break;

            String text = p.getText();

            if (text == null) continue;

            text = normalize(text);

            if (!text.isBlank()) {
                lastTitle = text;
            }
        }

        return lastTitle;
    }

    private String normalize(String s) {

        if (s == null) return "";

        return s
                .trim()
                .replaceAll("\\s+", " ");
    }
}