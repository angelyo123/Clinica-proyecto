package com.historias_clinicas.hc.entidades;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "historia_clinica_version")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class HistoriaClinicaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer numeroVersion;  // 1, 2, 3...
    private String descripcion;     // "Versión inicial", "Editado", etc.

    @ManyToOne
    @JoinColumn(name = "hc_id")
    private HistoriaClinica historiaClinica;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampoValor> valores;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL)
    private List<DocumentoAdjunto> documentos;

    @Lob
    @Column(name = "word_final")
    private byte[] wordFinal;

    @Lob
    @Column(name = "pdf_final")
    private byte[] pdfFinal;


    private java.time.LocalDateTime fechaCreacion;
}