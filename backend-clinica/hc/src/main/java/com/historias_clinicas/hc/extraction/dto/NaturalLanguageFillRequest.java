package com.historias_clinicas.hc.extraction.dto;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import lombok.Data;

import java.util.List;

@Data
public class NaturalLanguageFillRequest {

    private String texto;

    private List<FieldDefinition> campos;

}