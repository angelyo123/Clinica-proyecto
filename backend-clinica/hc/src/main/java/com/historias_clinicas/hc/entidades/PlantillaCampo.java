package com.historias_clinicas.hc.entidades;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private PlantillaSeccion seccion;

    /**
     * Nombre lógico del campo (edad, relato, fc, etc.)
     */
    private String nombreCampo;

    /**
     * Texto original detectado en la plantilla (etiqueta o contenido)
     */
    private String textoOriginal;

    /**
     * TIPO DE CAMPO DETECTADO
     * Ejemplos: linea_punteada, celda_vacia, subrayado, puntos
     */
    private String tipoCampo;

    /**
     * POSICIÓN EXACTA EN EL WORD
     * Permite volver al párrafo exacto y reemplazar runs.
     */

    private String descripcionCampo;

    private Integer indexParrafo;

    private Integer indexRunInicio;

    private Integer indexRunFin;
    private Integer itemIndex;

    /** Si está dentro de tabla */
    private Integer indexTabla;      // null si está fuera
    private Integer indexFila;
    private Integer indexCelda;

}