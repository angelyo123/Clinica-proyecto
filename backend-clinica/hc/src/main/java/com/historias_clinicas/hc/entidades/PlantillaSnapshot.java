package com.historias_clinicas.hc.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlantillaSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long plantillaId;

    @Column(length = 64, nullable = false)
    private String docHash;

    @Column(length = 30, nullable = false)
    private String modelVersion;

    private LocalDateTime createdAt;
}