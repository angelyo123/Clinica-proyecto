package com.historias_clinicas.hc.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CognitiveCell {

    private String stableId;
    private String text;

    private String leftContext;
    private String topContext;
    private String sectionTitle;

    private String blockType;

    private int bodyIndex;
    private int row;
    private int col;

    private int rowSpan;
    private int colSpan;

    private String rightContext;
    private String bottomContext;

    private int depth;

    private boolean empty;
}