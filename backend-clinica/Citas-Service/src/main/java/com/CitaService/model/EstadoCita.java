package com.CitaService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "estado_cita")
public class EstadoCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;  // Ej: "PENDIENTE", "CONFIRMADA", "EN_PROCESO", "CANCELADA"

    @Column(nullable = false)
    private String nombre;  // Ej: "Pendiente de confirmación"

    private String descripcion;
    private Boolean activo = true;
}