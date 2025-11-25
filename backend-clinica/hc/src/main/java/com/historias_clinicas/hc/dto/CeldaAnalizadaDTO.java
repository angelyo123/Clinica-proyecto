package com.historias_clinicas.hc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CeldaAnalizadaDTO {

    private Integer tabla;
    private Integer fila;
    private Integer columna;

    // Texto completo de la celda
    private String textoCompleto;

    // Palabras con su posición exacta
    private List<PalabraDTO> palabras;
}