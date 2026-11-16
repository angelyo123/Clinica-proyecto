package com.historias_clinicas.hc.visual;

import java.util.ArrayList;
import java.util.List;

public class CellVisual {

    private int rowStart;
    private int colStart;

    private int rowSpan;
    private int colSpan;

    private boolean master;
    private CellVisual masterRef;

    private String text;
    private String stableId;
    private boolean empty;
    private List<String> runs = new ArrayList<>();

    // contenido anidado
    private final List<TableBlock> nestedTables = new ArrayList<>();
    private final List<ParagraphBlock> nestedParagraphs = new ArrayList<>();

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public List<String> getRuns() {
        return runs;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }


    public boolean isEmpty() {
        return empty;
    }
    public CellVisual(int rowStart, int colStart,
                      int rowSpan, int colSpan,
                      boolean master,
                      String text) {

        this.rowStart = rowStart;
        this.colStart = colStart;
        this.rowSpan = rowSpan;
        this.colSpan = colSpan;
        this.master = master;
        this.text = text;
    }

    public boolean isMaster() { return master; }
    public void setMasterRef(CellVisual ref) { this.masterRef = ref; }
    public CellVisual getMasterRef() { return masterRef; }

    public int getRowStart() { return rowStart; }
    public int getColStart() { return colStart; }

    public int getRowSpan() { return rowSpan; }
    public int getColSpan() { return colSpan; }

    public String getText() { return text; }

    public void setStableId(String id) { this.stableId = id; }
    public String getStableId() { return stableId; }

    // ✅ getters necesarios
    public List<TableBlock> getNestedTables() {
        return nestedTables;
    }

    public List<ParagraphBlock> getNestedParagraphs() {
        return nestedParagraphs;
    }

    public CellVisual getMaster() {

        if (master) return this;

        return masterRef != null ? masterRef : this;
    }
}