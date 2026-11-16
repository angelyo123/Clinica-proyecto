package com.historias_clinicas.hc.visual;

public final class ParagraphBlock implements VisualBlock {

    private int bodyIndex;
    private int visualIndex;
    private String path;

    private String text;

    public ParagraphBlock(int bodyIndex, int visualIndex, String path, String text) {
        this.bodyIndex = bodyIndex;
        this.visualIndex = visualIndex;
        this.path = path;
        this.text = text;
    }

    public int getBodyIndex() { return bodyIndex; }

    @Override
    public int getVisualIndex() { return visualIndex; }

    @Override
    public String getPath() { return path; }

    public String getText() { return text; }
}