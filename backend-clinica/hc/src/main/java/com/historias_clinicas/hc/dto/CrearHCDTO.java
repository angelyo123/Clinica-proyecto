package com.historias_clinicas.hc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearHCDTO {
    private Long pacienteId;
    private Long medicoId;
    private Long plantillaId;
}