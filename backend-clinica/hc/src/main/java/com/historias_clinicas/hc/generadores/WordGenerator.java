package com.historias_clinicas.hc.generadores;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WordGenerator {

    /**
     * Genera un documento Word reemplazando los campos detectados por IA.
     *
     * valores:
     *   key   → PlantillaCampo (tiene textoOriginal + coordenadas)
     *   value → texto COMPLETADO (lo que vino en jsonConfirmar)
     */
    public byte[] generarDocumento(byte[] plantillaBytes,
                                   Map<PlantillaCampo, String> valores) throws Exception {

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(plantillaBytes))) {

            for (Map.Entry<PlantillaCampo, String> entry : valores.entrySet()) {

                PlantillaCampo campo = entry.getKey();
                String valorCompletado = entry.getValue();

                // Si es nulo o vacío, no hacemos nada
                if (valorCompletado == null || valorCompletado.isBlank()) {
                    continue;
                }

                String textoOriginal = campo.getTextoOriginal();
                aplicarValorEnDocumento(doc, campo, textoOriginal, valorCompletado);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    // =========================================================================
    // APLICAR VALOR: por coordenadas y con fallback global
    // =========================================================================
    private void aplicarValorEnDocumento(XWPFDocument doc,
                                         PlantillaCampo campo,
                                         String textoOriginal,
                                         String textoCompletado) {

        // Normalizamos nulls
        if (textoOriginal == null) textoOriginal = "";
        textoOriginal = textoOriginal.trim();

        // 1) Intentar por coordenadas (tabla/fila/celda o párrafo)
        boolean aplicado = false;

        XWPFParagraph pCoord = obtenerParrafoPorCoordenadas(doc, campo);
        if (pCoord != null) {
            aplicado = reemplazarEnParrafo(pCoord, textoOriginal, textoCompletado);
        }

        // 2) Si por coordenadas no funcionó, intentar búsqueda global
        if (!aplicado && !textoOriginal.isBlank()) {
            buscarYReemplazarGlobal(doc, textoOriginal, textoCompletado);
        }
    }

    // =========================================================================
    // OBTENER PÁRRAFO POR COORDENADAS
    // =========================================================================
    private XWPFParagraph obtenerParrafoPorCoordenadas(XWPFDocument doc, PlantillaCampo campo) {
        try {
            if (campo.getIndexTabla() == null) {
                // ---- Caso: párrafo normal (sin tabla) ----
                Integer idxP = campo.getIndexParrafo();
                if (idxP == null) return null;
                List<XWPFParagraph> paragraphs = doc.getParagraphs();
                if (idxP < 0 || idxP >= paragraphs.size()) return null;
                return paragraphs.get(idxP);
            } else {
                // ---- Caso: dentro de tabla ----
                int idxTabla = campo.getIndexTabla();
                int idxFila = campo.getIndexFila() != null ? campo.getIndexFila() : 0;
                int idxCelda = campo.getIndexCelda() != null ? campo.getIndexCelda() : 0;

                List<XWPFTable> tablas = doc.getTables();
                if (idxTabla < 0 || idxTabla >= tablas.size()) return null;

                XWPFTable tabla = tablas.get(idxTabla);
                if (idxFila < 0 || idxFila >= tabla.getNumberOfRows()) return null;

                XWPFTableRow fila = tabla.getRow(idxFila);
                if (fila == null || idxCelda < 0 || idxCelda >= fila.getTableCells().size()) return null;

                XWPFTableCell celda = fila.getCell(idxCelda);
                if (celda == null || celda.getParagraphs().isEmpty()) return null;

                // Tomamos el primer párrafo de la celda
                return celda.getParagraphArray(0);
            }
        } catch (Exception e) {
            // Si algo revienta, devolvemos null para que el Fallback global lo intente
            return null;
        }
    }

    // =========================================================================
    // REEMPLAZO EN PÁRRAFO (textoOriginal → textoCompletado)
    // =========================================================================
    private boolean reemplazarEnParrafo(XWPFParagraph p,
                                        String textoOriginal,
                                        String textoCompletado) {

        // Unificamos los runs para trabajar sobre un solo string
        String textoParrafo = getParagraphText(p);
        if (textoParrafo == null) textoParrafo = "";

        String base = textoParrafo;
        String originalNorm = normalizarSuave(textoOriginal);
        String baseNorm = normalizarSuave(base);

        if (!originalNorm.isEmpty() && baseNorm.contains(originalNorm)) {
            // Reemplazo normal
            String nuevoTexto = reemplazarNormalizado(base, textoOriginal, textoCompletado);
            setParagraphText(p, nuevoTexto);
            return true;
        }

        // Si no se encuentra el textoOriginal, como fallback
        // podemos sobreescribir el párrafo completo con el textoCompletado
        if (textoOriginal.isBlank()) {
            setParagraphText(p, textoCompletado);
            return true;
        }

        return false;
    }

    // Lee todo el texto concatenado de un párrafo
    private String getParagraphText(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun r : p.getRuns()) {
            String t = r.toString();
            if (t != null) {
                sb.append(t);
            }
        }
        return sb.toString();
    }

    // Reemplaza textoOriginal dentro de base, usando versión normalizada como guía
    private String reemplazarNormalizado(String base,
                                         String original,
                                         String reemplazo) {

        // Intento directo primero (si coincide exacto, mejor)
        if (base.contains(original)) {
            return base.replace(original, reemplazo);
        }

        // Si la versión exacta falla, intentar por normalización
        String baseNorm = normalizarSuave(base);
        String originalNorm = normalizarSuave(original);

        int idx = baseNorm.indexOf(originalNorm);
        if (idx < 0) {
            // No se encuentra → devolvemos base sin cambios
            return base;
        }

        // Hacemos reemplazo aproximado usando los índices en el string original
        // Para no complicarnos mucho, sobreescribimos todo el párrafo:
        return reemplazo;
    }

    // Sobreescribe los runs de un párrafo con un solo run usando el texto dado
    private void setParagraphText(XWPFParagraph p, String texto) {

        // Guardamos formato del primer run ANTES de eliminar nada
        XWPFRun runRef = null;
        if (!p.getRuns().isEmpty()) {
            runRef = p.getRuns().get(0);
        }

        // 1. Creamos un nuevo run, ANTES de borrar los otros
        XWPFRun nuevo = p.createRun();
        nuevo.setText(texto);

        // 2. Copiamos formato del runRef solo si sigue conectado
        if (runRef != null) {
            try {
                // Intentamos leer una propiedad para verificar que NO esté desconectado
                String family = runRef.getFontFamily(); // si está huérfano lanza excepción

                if (family != null) nuevo.setFontFamily(family);

                int size = runRef.getFontSize();
                if (size > 0) nuevo.setFontSize(size);

                nuevo.setBold(runRef.isBold());
                nuevo.setItalic(runRef.isItalic());
                nuevo.setColor(runRef.getColor());

            } catch (Exception ex) {
                // Si el run está desconectado, no copiamos formato
                System.out.println("⚠ No se pudo copiar formato de runRef por estar desconectado.");
            }
        }


        // 3. Ahora sí, eliminamos todos los runs anteriores (menos el nuevo)
        // Nota: hay que eliminar empezando desde el principio
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            XWPFRun r = p.getRuns().get(i);
            if (r != nuevo) {
                p.removeRun(i);
            }
        }
    }


    // =========================================================================
    // BÚSQUEDA GLOBAL (FALLBACK) — si las coordenadas fallan
    // =========================================================================
    private void buscarYReemplazarGlobal(XWPFDocument doc,
                                         String textoOriginal,
                                         String textoCompletado) {

        String originalNorm = normalizarSuave(textoOriginal);

        if (originalNorm.isBlank()) {
            return;
        }

        // 1) Buscar en párrafos fuera de tablas
        for (XWPFParagraph p : doc.getParagraphs()) {
            String base = getParagraphText(p);
            String baseNorm = normalizarSuave(base);

            if (baseNorm.contains(originalNorm)) {
                String nuevo = reemplazarNormalizado(base, textoOriginal, textoCompletado);
                setParagraphText(p, nuevo);
                return;
            }
        }

        // 2) Buscar dentro de tablas
        for (XWPFTable tabla : doc.getTables()) {
            for (XWPFTableRow fila : tabla.getRows()) {
                for (XWPFTableCell celda : fila.getTableCells()) {
                    for (XWPFParagraph p : celda.getParagraphs()) {
                        String base = getParagraphText(p);
                        String baseNorm = normalizarSuave(base);

                        if (baseNorm.contains(originalNorm)) {
                            String nuevo = reemplazarNormalizado(base, textoOriginal, textoCompletado);
                            setParagraphText(p, nuevo);
                            return;
                        }
                    }
                }
            }
        }
    }


    // =========================================================================
    // NORMALIZACIÓN SUAVE (no destruimos caracteres, solo acentos y espacios)
    // =========================================================================
    private String normalizarSuave(String s) {
        if (s == null) return "";
        s = s.trim();
        // Normalizar acentos y compatibilidad, pero sin matar símbolos raros tipo º, °
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        // Convertir múltiples espacios en uno solo
        s = s.replaceAll("\\s+", " ");
        return s;
    }
}