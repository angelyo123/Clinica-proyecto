package com.historias_clinicas.hc.visual;

import java.util.ArrayList;
import java.util.List;

public class VisualDocument {

    private final List<VisualBlock> blocks = new ArrayList<>();

    public List<VisualBlock> getBlocks() {
        return blocks;
    }

    public void addBlock(VisualBlock block) {
        blocks.add(block);
    }
}