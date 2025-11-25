package com.historias_clinicas.hc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PalabraDTO {
    private String texto;
    private int runIndex;
    private int wordIndex; // posición de la palabra dentro del run
}