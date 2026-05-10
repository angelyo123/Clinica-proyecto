package com.historias_clinicas.hc.visual;

import org.apache.commons.codec.digest.DigestUtils;

public class generarIds {

    public void generarIds(VisualDocument doc) {

        for (VisualBlock block : doc.getBlocks()) {

            if (block instanceof TableBlock tb) {

                TableGrid grid = tb.getGrid();
                CellVisual[][] matrix = grid.getMatrix();

                for (int r = 0; r < grid.getTotalRows(); r++) {
                    for (int c = 0; c < grid.getTotalCols(); c++) {

                        CellVisual cell = matrix[r][c];

                        if (cell != null && cell.isMaster()) {

                            String base =
                                    "B" + tb.getBodyIndex() +
                                            "T" + tb.getTableIndex() +
                                            "R" + r +
                                            "C" + c;

                            String contexto =
                                    obtenerContextoFila(matrix, r);

                            String hash =
                                    DigestUtils.sha1Hex(contexto);

                            cell.setStableId(base + "#" + hash.substring(0,8));
                        }
                    }
                }
            }
        }
    }

    private String obtenerContextoFila(CellVisual[][] matrix, int fila) {

        StringBuilder sb = new StringBuilder();

        // Fila anterior
        if (fila - 1 >= 0) {
            sb.append(extraerTextoFila(matrix[fila - 1]));
        }

        // Fila actual
        sb.append(extraerTextoFila(matrix[fila]));

        // Fila siguiente
        if (fila + 1 < matrix.length) {
            sb.append(extraerTextoFila(matrix[fila + 1]));
        }

        return sb.toString();
    }

    private String extraerTextoFila(CellVisual[] fila) {

        StringBuilder sb = new StringBuilder();

        for (CellVisual cell : fila) {
            if (cell != null && cell.isMaster()) {
                sb.append(cell.getText()).append("|");
            }
        }

        return sb.toString();
    }
}
