package com.historias_clinicas.hc.dto;

import lombok.Data;

@Data
public class CampoValorConfirmado {

    private String valor;
    private String nombre; // opcional, pero útil para debug

    private Integer indexTabla;
    private Integer indexFila;
    private Integer indexCelda;
    private Integer indexParrafo;

}