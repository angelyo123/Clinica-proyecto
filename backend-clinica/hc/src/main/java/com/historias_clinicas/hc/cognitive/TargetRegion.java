package com.historias_clinicas.hc.cognitive;

import lombok.Data;

@Data
public class TargetRegion {

    private String mode;

    private String anchorText;
    private String secondAnchorText;

    private Integer relativeRow;
    private Integer relativeCol;

    private String reference;
    private String hint;
}