package com.historias_clinicas.hc.entidades;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "historia_clinica")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pacienteId;   // viene de Paciente-Service
    private Long medicoId;     // viene de Medico-Service

    @ManyToOne
    @JoinColumn(name = "plantilla_id")
    private Plantilla plantilla;

    private String estado; // "BORRADOR", "APROBADO"

    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL)
    private List<HistoriaClinicaVersion> versiones;
}