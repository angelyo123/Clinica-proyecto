package com.historias_clinicas.hc.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Plantilla plantilla;

    private Integer indexTabla;
    private Integer indexFila;
    private Integer indexColumna;

    @Column(columnDefinition = "TEXT")
    private String textoOriginal;

    @Enumerated(EnumType.STRING)
    private TipoAccion tipoAccion;
    // REESCRIBIR_TEXTO | MARCAR_CELDA | NO_TOCAR

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String hashEstructura;
}

