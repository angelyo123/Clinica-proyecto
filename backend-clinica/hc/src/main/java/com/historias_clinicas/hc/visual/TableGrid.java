package com.historias_clinicas.hc.visual;

public class TableGrid {

    private int totalRows;
    private int totalCols;
    private CellVisual[][] matrix;

    public TableGrid(int totalRows, int totalCols) {
        this.totalRows = totalRows;
        this.totalCols = totalCols;
        this.matrix = new CellVisual[totalRows][totalCols];
    }

    public CellVisual[][] getMatrix() {
        return matrix;
    }

    public int getTotalRows() { return totalRows; }
    public int getTotalCols() { return totalCols; }

    public void setCell(int r, int c, CellVisual cell) {
        matrix[r][c] = cell;
    }
}