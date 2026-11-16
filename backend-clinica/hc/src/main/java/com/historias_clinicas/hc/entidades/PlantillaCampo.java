package com.historias_clinicas.hc.entidades;
import jakarta.persistence.*;
import lombok.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long plantillaId;



    @Column(length = 500, nullable = false)
    private String descripcion;

    @Column(length = 30, nullable = false)
    private String tipoDato;

    @Column(length = 50)
    private String geom;

    @Column(length = 50)
    private String blockHash;

    @Column(length = 50)
    private String anchorHash;

    private Long snapshotId;

    @Column(length = 120, nullable = false)
    private String stableId;

    @Column(length = 120)
    private String readStableId;


    private String interactionType;
    private String preservationMode;
    private String targetSemantics;

    private String writeMode;
    private String writeValue;
    private String executionHint;

    private String targetRegionMode;
    private String anchorText;
    private String secondAnchorText;
    private Integer relativeRow;
    private Integer relativeCol;
    private String reference;
    private String targetHint;
    private String rewriteText;


    // 🔹 ESTA ES LA RELACIÓN QUE FALTA
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id")
    private PlantillaSeccion seccion;
}