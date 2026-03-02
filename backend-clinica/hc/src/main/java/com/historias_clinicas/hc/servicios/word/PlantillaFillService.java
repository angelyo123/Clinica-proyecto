package com.historias_clinicas.hc.servicios.word;

import com.historias_clinicas.hc.dto.CampoValorConfirmado;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantillaFillService {

    public byte[] llenarWord(
            byte[] plantillaBytes,
            List<CampoValorConfirmado> valores
    ) throws Exception {

        XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantillaBytes)
        );

        for (CampoValorConfirmado v : valores) {

            // ===============================
            // CASO 1: CELDA DE TABLA
            // indexCelda aquí se interpreta como COLUMNA VISUAL (columnaMarcable)
            // ===============================
            if (v.getIndexTabla() != null &&
                    v.getIndexFila()  != null &&
                    v.getIndexCelda() != null) {

                XWPFTable tabla = doc.getTableArray(v.getIndexTabla());
                if (tabla == null) continue;

                XWPFTableRow fila = tabla.getRow(v.getIndexFila());
                if (fila == null) continue;

                // ✅ Convertir columnaVisual -> celda real (respeta merges/gridspan)
                XWPFTableCell celda = getCellByColumnaVisual(fila, v.getIndexCelda());
                if (celda == null) continue;

                String actual = celda.getText();

                // ✅ Si ya está marcada y estamos intentando marcar "X", no reescribir
                if (celdaYaTieneMarca(actual) &&
                        "X".equalsIgnoreCase(String.valueOf(v.getValor()))) {
                    continue;
                }

                escribirCeldaSeguro(celda, v.getValor());
            }

            // ===============================
            // CASO 2: PÁRRAFO
            // ===============================
            else if (v.getIndexParrafo() != null) {

                XWPFParagraph p = doc.getParagraphArray(v.getIndexParrafo());
                if (p == null) continue;

                // limpiar runs de forma segura
                int runs = p.getRuns() == null ? 0 : p.getRuns().size();
                for (int i = runs - 1; i >= 0; i--) {
                    p.removeRun(i);
                }

                p.createRun().setText(
                        v.getValor() != null ? v.getValor() : ""
                );
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void escribirCeldaSeguro(XWPFTableCell cell, String texto) {

        // Asegurar al menos 1 párrafo
        if (cell.getParagraphs() == null || cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }

        // Limpiar runs de todos los párrafos existentes
        for (XWPFParagraph p : cell.getParagraphs()) {
            int runs = p.getRuns() == null ? 0 : p.getRuns().size();
            for (int i = runs - 1; i >= 0; i--) {
                p.removeRun(i);
            }
        }

        // Usar el primer párrafo
        XWPFParagraph p0 = cell.getParagraphs().get(0);
        XWPFRun run = p0.createRun();
        run.setText(texto != null ? texto : "");
    }

    /**
     * Convierte una "columna visual" (basada en gridSpan) a la celda real de POI.
     * Esto es CRÍTICO cuando tu pipeline calcula columnaMarcable con columnaVisual.
     */
    private XWPFTableCell getCellByColumnaVisual(XWPFTableRow row, int columnaVisualBuscada) {

        int colVis = 0;

        for (XWPFTableCell cell : row.getTableCells()) {

            int spanH = 1;

            var tcPr = cell.getCTTc().getTcPr();
            if (tcPr != null && tcPr.getGridSpan() != null) {
                spanH = tcPr.getGridSpan().getVal().intValue();
            }

            // La celda cubre el rango [colVis, colVis + spanH - 1]
            if (columnaVisualBuscada >= colVis && columnaVisualBuscada < colVis + spanH) {
                return cell;
            }

            colVis += spanH;
        }

        return null;
    }

    private boolean celdaYaTieneMarca(String txt) {
        if (txt == null) return false;
        String t = txt.trim();
        return t.equalsIgnoreCase("x") || t.equals("✔") || t.equals("✓");
    }
}