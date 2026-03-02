package com.historias_clinicas.hc.entidades;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarRequest {
    private Map<String, String> valoresPlano;
    private String textoClinico;
}
