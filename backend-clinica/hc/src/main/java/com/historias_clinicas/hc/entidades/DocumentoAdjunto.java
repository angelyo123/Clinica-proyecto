package com.historias_clinicas.hc.entidades;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hc_documento_adjunto")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DocumentoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;            // donde se almacena la foto o pdf
    private String tipo;           // "image/jpg", "application/pdf"
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "version_id")
    private HistoriaClinicaVersion version;

    private LocalDateTime fechaSubida;
}