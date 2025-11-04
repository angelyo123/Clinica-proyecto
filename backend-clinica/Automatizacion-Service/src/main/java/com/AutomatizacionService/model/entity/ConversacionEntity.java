package com.AutomatizacionService.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pacienteId;

    @Column(columnDefinition = "TEXT")
    private String mensajeUsuario;

    @Column(columnDefinition = "TEXT")
    private String mensajeIA;

    @Column(columnDefinition = "TEXT")
    private String dataJson;

    private LocalDateTime fecha;

    // ✅ Constructor personalizado para guardar mensajes fácilmente
    public ConversacionEntity(Long pacienteId, String mensajeUsuario, String mensajeIA, String dataJson) {
        this.pacienteId = pacienteId;
        this.mensajeUsuario = mensajeUsuario;
        this.mensajeIA = mensajeIA;
        this.dataJson = dataJson;
        this.fecha = LocalDateTime.now();
    }
}
