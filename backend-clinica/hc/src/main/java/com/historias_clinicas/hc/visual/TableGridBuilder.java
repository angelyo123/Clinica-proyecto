package com.historias_clinicas.hc.visual;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.util.ArrayList;
import java.util.List;

public final class TableGridBuilder {


    public TableGrid build(XWPFTable tabla) {
        return build(tabla, 0, "T0");
    }

    private TableGrid build(XWPFTable tabla, int depth, String tablePath) {

        int totalRows = tabla.getNumberOfRows();

        int totalCols = resolveColumnCount(tabla);

        TableGrid grid = new TableGrid(totalRows, totalCols);
        boolean[][] occupied = new boolean[totalRows][totalCols];

        for (int r = 0; r < totalRows; r++) {

            XWPFTableRow row = tabla.getRow(r);
            int colPointer = 0;

            for (XWPFTableCell physicalCell : row.getTableCells()) {

                while (colPointer < totalCols && occupied[r][colPointer]) {
                    colPointer++;
                }

                int colSpan = getColSpan(physicalCell);
                int rowSpan = 1;

                if (isVMergeRestart(physicalCell)) {

                    rowSpan = calcularSpanVerticalReal(
                            tabla,
                            r,
                            colPointer,
                            totalCols
                    );
                }

                String text = normalizeCellText(physicalCell.getText());

                boolean empty = text.isBlank();

                CellVisual master = new CellVisual(
                        r,
                        colPointer,
                        rowSpan,
                        colSpan,
                        true,
                        text
                );

                String cellPath = tablePath + "/R" + r + "C" + colPointer;

                master.setPath(cellPath);

                master.setEmpty(empty);

                // parse contenido interno
                parseCellContent(physicalCell, master, depth);

                for (int i = 0; i < rowSpan; i++) {
                    for (int j = 0; j < colSpan; j++) {

                        int rr = r + i;
                        int cc = colPointer + j;

                        if (rr >= totalRows || cc >= totalCols) continue;

                        occupied[rr][cc] = true;

                        if (i == 0 && j == 0) {

                            grid.setCell(rr, cc, master);

                        } else {

                            CellVisual cont = new CellVisual(
                                    rr,
                                    cc,
                                    1,
                                    1,
                                    false,
                                    ""
                            );

                            cont.setMasterRef(master);
                            grid.setCell(rr, cc, cont);
                        }
                    }
                }

                colPointer += colSpan;
            }

            // rellenar slots faltantes en la fila
            while (colPointer < totalCols) {

                if (!occupied[r][colPointer]) {

                    CellVisual filler = new CellVisual(
                            r,
                            colPointer,
                            1,
                            1,
                            true,
                            ""
                    );

                    filler.setEmpty(true);

                    grid.setCell(r, colPointer, filler);
                    occupied[r][colPointer] = true;
                }

                colPointer++;
            }
        }


        validarContinuations(grid);

        // VALIDACIÓN FINAL DEL GRID
        validateGrid(grid.getMatrix());

        return grid;
    }

    private boolean isVMergeRestart(XWPFTableCell cell) {
        var tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null || tcPr.getVMerge() == null) return false;

        var val = tcPr.getVMerge().getVal();
        return val == STMerge.RESTART;
    }

    private boolean isVMergeContinue(XWPFTableCell cell) {
        var tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null || tcPr.getVMerge() == null) return false;

        var val = tcPr.getVMerge().getVal();
        return val == null || val == STMerge.CONTINUE;
    }

    private int getColSpan(XWPFTableCell cell) {
        var tcPr = cell.getCTTc().getTcPr();
        if (tcPr != null && tcPr.getGridSpan() != null) {
            return tcPr.getGridSpan().getVal().intValue();
        }
        return 1;
    }

    private int calcularSpanVerticalReal(
            XWPFTable tabla,
            int rowStart,
            int colStartSlot,
            int totalCols
    ) {

        int span = 1;

        for (int rr = rowStart + 1; rr < tabla.getNumberOfRows(); rr++) {

            XWPFTableRow nextRow = tabla.getRow(rr);

            XWPFTableCell physicalCell =
                    findPhysicalCellAtSlot(nextRow, colStartSlot, totalCols);

            if (physicalCell == null) break;

            if (isVMergeContinue(physicalCell)) {
                span++;
            } else {
                break;
            }
        }

        return span;
    }

    private XWPFTableCell findPhysicalCellAtSlot(
            XWPFTableRow row,
            int targetSlot,
            int totalCols
    ) {

        int colPointer = 0;

        for (XWPFTableCell cell : row.getTableCells()) {

            int colSpan = getColSpan(cell);

            if (targetSlot >= colPointer &&
                    targetSlot < colPointer + colSpan) {
                return cell;
            }

            colPointer += colSpan;
        }

        return null;
    }
    private String normalizeCellText(String t) {

        if (t == null) return "";

        return t
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void validarContinuations(TableGrid grid) {

        for (int r = 0; r < grid.getTotalRows(); r++) {
            for (int c = 0; c < grid.getTotalCols(); c++) {

                CellVisual cell = grid.getMatrix()[r][c];

                if (cell == null) continue;

                if (!cell.isMaster() && cell.getMasterRef() == null) {
                    throw new IllegalStateException(
                            "Continuation sin master en R" + r + "C" + c
                    );
                }
            }
        }
    }

    private void parseCellContent(
            XWPFTableCell physicalCell,
            CellVisual visualCell,
            int depth
    ) {

        if (depth > 5) return;

        // 🔹 Capturar runs reales
        for (XWPFParagraph p : physicalCell.getParagraphs()) {

            if (p.getRuns() != null) {
                for (XWPFRun run : p.getRuns()) {

                    String t = run.text();

                    if (t != null && !t.isBlank()) {
                        visualCell.getRuns().add(t);
                    }
                }
            }
        }

        // 🔹 Parsear contenido estructural
        for (IBodyElement el : physicalCell.getBodyElements()) {

            if (el instanceof XWPFParagraph p) {

                String text = normalizeCellText(p.getText());

                ParagraphBlock paragraph = new ParagraphBlock(
                        -1,
                        -1,
                        "NESTED",
                        text
                );

                visualCell.getNestedParagraphs().add(paragraph);
            }

            if (el instanceof XWPFTable t) {

                String nestedPath = visualCell.getPath() + "/T" + visualCell.getNestedTables().size();

                TableGrid nestedGrid = build(
                        t,
                        depth + 1,
                        nestedPath
                );

                TableBlock nestedTable = new TableBlock(
                        -1,
                        -1,
                        -1,
                        "NESTED",
                        nestedGrid,
                        depth + 1
                );

                visualCell.getNestedTables().add(nestedTable);
            }
        }
    }

    private void validateGrid(CellVisual[][] grid) {

        if (grid == null || grid.length == 0) {
            throw new IllegalStateException("Grid vacío");
        }

        int cols = grid[0].length;

        for (int r = 0; r < grid.length; r++) {

            if (grid[r].length != cols) {
                throw new IllegalStateException(
                        "Grid inconsistente en fila " + r
                );
            }

            for (int c = 0; c < cols; c++) {

                if (grid[r][c] == null) {
                    throw new IllegalStateException(
                            "Celda null en " + r + "," + c
                    );
                }
            }
        }
    }

    private int resolveColumnCount(XWPFTable table) {

        CTTbl ctTbl = table.getCTTbl();

        if (ctTbl.getTblGrid() != null) {
            return ctTbl.getTblGrid().sizeOfGridColArray();
        }

        int max = 0;

        for (XWPFTableRow row : table.getRows()) {

            int cols = 0;

            for (XWPFTableCell cell : row.getTableCells()) {

                int span = getColSpan(cell);

                cols += span;
            }

            max = Math.max(max, cols);
        }

        return max;
    }
}