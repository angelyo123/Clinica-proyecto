package com.AutomatizacionService.model.entity.medico;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class RegistroIAMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long medicoId;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    private LocalDateTime fecha = LocalDateTime.now();

    // 🔹 Constructores
    public RegistroIAMedico() {}

    public RegistroIAMedico(Long medicoId, String mensaje, String respuesta) {
        this.medicoId = medicoId;
        this.mensaje = mensaje;
        this.respuesta = respuesta;
    }

    // 🔹 Getters y Setters
    public Long getId() {
        return id;
    }

    public Long getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(Long medicoId) {
        this.medicoId = medicoId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    @Override
    public String toString() {
        return "RegistroIAMedico{" +
                "id=" + id +
                ", medicoId=" + medicoId +
                ", mensaje='" + mensaje + '\'' +
                ", respuesta='" + respuesta + '\'' +
                ", fecha=" + fecha +
                '}';
    }
}