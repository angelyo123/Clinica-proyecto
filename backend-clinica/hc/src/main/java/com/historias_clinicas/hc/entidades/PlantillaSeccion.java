package com.historias_clinicas.hc.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "plantilla_seccion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;  // "ANAMNESIS", "DIAGNOSTICO", etc.

    @ManyToOne
    @JoinColumn(name = "plantilla_id", nullable = false)
    private Plantilla plantilla;

    @OneToMany(mappedBy = "seccion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlantillaCampo> campos;
}