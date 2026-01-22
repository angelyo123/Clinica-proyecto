package com.historias_clinicas.hc.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"plantilla_id", "hashEstructura"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaVision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Plantilla plantilla;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> visionJson;

    @Column(nullable = false)
    private String hashEstructura;

    @Column(nullable = false)
    private LocalDateTime creadoEn;
}