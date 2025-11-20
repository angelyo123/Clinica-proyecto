package com.historias_clinicas.hc.servicios;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extrae bloques lógicos (secciones) de un documento Word.
 * Úsalo ANTES de enviar contenido a la IA.
 * La detección de títulos es heurística y funciona para plantillas clínicas reales.
 */
@Slf4j
@Service
public class BlockExtractorService {

    // ----------------------------
    // MODELO DE SECCIÓN/BLOQUE
    // ----------------------------
    @Data
    @AllArgsConstructor
    public static class BloqueSeccion {
        private String titulo;     // Ej: "ENFERMEDAD ACTUAL"
        private String contenido;  // Texto dentro de la sección
        private int orden;         // Orden en el documento
    }

    // =====================================================================
    // EXTRAER BLOQUES (este es el método principal que usarás)
    // =====================================================================
    public List<BloqueSeccion> extraerBloques(XWPFDocument doc) {

        List<BloqueSeccion> bloques = new ArrayList<>();

        // Estado actual
        String tituloActual = "GLOBAL";
        StringBuilder buffer = new StringBuilder();
        int orden = 0;

        // -----------------------------
        // 1) Párrafos normales del Word
        // -----------------------------
        for (XWPFParagraph p : doc.getParagraphs()) {
            ResultadoProcesado r = procesarParrafo(p, tituloActual, buffer, orden);

            if (r.huboCambioDeTitulo) {
                // Guardamos bloque previo (si tenía contenido)
                if (!r.contenidoPrevio.isBlank()) {
                    bloques.add(new BloqueSeccion(tituloActual, r.contenidoPrevio.trim(), orden));
                    orden++;
                }

                // Ahora cambiamos al nuevo título
                tituloActual = r.nuevoTitulo;

                // Reset buffer
                buffer.setLength(0);
            }

            // Continuamos con el buffer actualizado
        }

        // -----------------------------
        // 2) Tablas completas
        // -----------------------------
        for (XWPFTable tabla : doc.getTables()) {
            for (XWPFTableRow fila : tabla.getRows()) {
                for (XWPFTableCell celda : fila.getTableCells()) {
                    for (XWPFParagraph p : celda.getParagraphs()) {

                        ResultadoProcesado r = procesarParrafo(p, tituloActual, buffer, orden);

                        if (r.huboCambioDeTitulo) {
                            if (!r.contenidoPrevio.isBlank()) {
                                bloques.add(new BloqueSeccion(tituloActual, r.contenidoPrevio.trim(), orden));
                                orden++;
                            }
                            tituloActual = r.nuevoTitulo;
                            buffer.setLength(0);
                        }
                    }
                }
            }
        }

        // -----------------------------
        // 3) Guardar último bloque
        // -----------------------------
        String contenidoFinal = buffer.toString().trim();
        if (!contenidoFinal.isBlank()) {
            bloques.add(new BloqueSeccion(tituloActual, contenidoFinal, orden));
        }

        return bloques;
    }

    // ================================================================
    // MODELO INTERNO QUE DICE SI HUBO CAMBIO DE TÍTULO EN UN PÁRRAFO
    // ================================================================
    @Data
    private static class ResultadoProcesado {
        final boolean huboCambioDeTitulo;
        final String nuevoTitulo;
        final String contenidoPrevio;
    }

    // ================================================================
    // PROCESAR CADA PÁRRAFO
    // ================================================================
    private ResultadoProcesado procesarParrafo(
            XWPFParagraph p,
            String tituloActual,
            StringBuilder buffer,
            int ordenActual
    ) {
        String linea = (p.getText() == null) ? "" : p.getText().trim();

        // si está vacío → solo agregamos salto
        if (linea.isBlank()) {
            buffer.append("\n");
            return new ResultadoProcesado(false, tituloActual, "");
        }

        // Ver si este párrafo es un título
        if (esTituloSeccion(linea)) {

            String tituloLimpio = limpiarTitulo(linea);

            log.debug("Detectado título: {}", tituloLimpio);

            // Guardar contenido previo ANTES de cambiar título
            String contenidoPrevio = buffer.toString();
            return new ResultadoProcesado(
                    true,
                    tituloLimpio,
                    contenidoPrevio
            );
        }

        // Si no es título → acumularlo como contenido
        buffer.append(linea).append("\n");
        return new ResultadoProcesado(false, tituloActual, "");
    }

    // ================================================================
    // HEURÍSTICA PARA DETECTAR TÍTULOS DE SECCIÓN
    // ================================================================
    private boolean esTituloSeccion(String linea) {

        String t = linea.trim();

        if (t.isBlank()) return false;

        // 1) Línea que termina en ":" y no es tan larga
        if (t.endsWith(":") && t.length() <= 80) return true;

        // 2) Formato "I.- ANAMNESIS"
        if (t.matches("^[IVXLC]+\\.-\\s+.*")) return true;

        // 3) Casi todo mayúsculas y menos de 6 palabras
        String limpio = t.replace(".", "").replace(":", "");
        boolean mayus = limpio.equals(limpio.toUpperCase());

        if (mayus && t.split("\\s+").length <= 6) return true;

        // 4) Líneas tipo "ENFERMEDAD ACTUAL" (todo mayúsculas)
        if (t.equals(t.toUpperCase()) && t.length() > 3) return true;

        return false;
    }

    // ================================================================
    // LIMPIAR UN TÍTULO
    // ================================================================
    private String limpiarTitulo(String t) {
        String s = t.trim();

        // quitar "I.- "
        s = s.replaceAll("^[IVXLC]+\\.-\\s*", "");

        // quitar ":" o "."
        s = s.replaceAll("[:.]+$", "");

        return s.trim();
    }
}
