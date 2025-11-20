package com.historias_clinicas.hc.entidades;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hc_campo_valor")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CampoValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "campo_id")
    private PlantillaCampo campo;

    @ManyToOne
    @JoinColumn(name = "version_id")
    private HistoriaClinicaVersion version;

    @Column(columnDefinition = "TEXT")
    private String valor;
}