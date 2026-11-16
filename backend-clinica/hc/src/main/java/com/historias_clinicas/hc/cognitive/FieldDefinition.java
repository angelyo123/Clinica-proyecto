package com.historias_clinicas.hc.cognitive;

import lombok.Data;

@Data
public class FieldDefinition {

    private String stableId;
    private String writeStableId;

    private String description;
    private String dataType;

    private String interactionType;
    private String preservationMode;
    private String targetSemantics; // opcional pero recomendable

    private String writeMode;   // TEXT | MARK
    private String writeValue;  // "X", texto, etc.

    // opcional pero recomendado
    private String executionHint;

    private TargetRegion targetRegion;

    private String rewriteText;
}