package com.historias_clinicas.hc.entidades;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "plantilla")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plantilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;        // "LOAYZA", "SBS 2025", etc.
    private String descripcion;
    private Boolean activo = true;

    @Column(columnDefinition = "bytea")
    private byte[] archivoOriginal;

    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlantillaSeccion> secciones;


}