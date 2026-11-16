package com.historias_clinicas.hc.visual;

public final class TableBlock implements VisualBlock {

    private int bodyIndex;
    private int tableIndex;

    private int visualIndex;
    private String path;

    private TableGrid grid;

    private int depth;

    public TableBlock(
            int bodyIndex,
            int tableIndex,
            int visualIndex,
            String path,
            TableGrid grid,
            int depth
    ) {
        this.bodyIndex = bodyIndex;
        this.tableIndex = tableIndex;
        this.visualIndex = visualIndex;
        this.path = path;
        this.grid = grid;
        this.depth = depth;
    }

    public TableGrid getGrid() { return grid; }

    public int getBodyIndex() { return bodyIndex; }

    public int getTableIndex() { return tableIndex; }

    public int getDepth() { return depth; }

    @Override
    public int getVisualIndex() { return visualIndex; }

    @Override
    public String getPath() { return path; }
}