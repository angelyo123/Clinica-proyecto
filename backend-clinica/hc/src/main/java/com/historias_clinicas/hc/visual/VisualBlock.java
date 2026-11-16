package com.historias_clinicas.hc.visual;

public sealed interface VisualBlock
        permits ParagraphBlock, TableBlock {

    String getPath();

    int getVisualIndex();
}