package com.historias_clinicas.hc.visual;

import org.junit.jupiter.api.Test;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class StableIdDeterminismTest {

    @Test
    void idsMustBeDeterministic() throws Exception {

        // Cargar documento de prueba
        XWPFDocument doc = new XWPFDocument(
                new FileInputStream("src/test/resources/historia_clinica.docx")
        );

        VisualDocumentBuilder builder = new VisualDocumentBuilder();
        StableIdGenerator generator = new StableIdGenerator();

        // Primera ejecución
        VisualDocument d1 = builder.build(doc);
        generator.generarIds(d1);

        // Segunda ejecución
        VisualDocument d2 = builder.build(doc);
        generator.generarIds(d2);

        Set<String> ids1 = extractIds(d1);
        Set<String> ids2 = extractIds(d2);

        // Sin estas dos comprobaciones, un documento sin tablas (dos conjuntos vacíos) pasaría la prueba
        assertFalse(ids1.isEmpty(), "El documento de prueba debería generar IDs");
        assertFalse(ids1.contains(null), "Toda celda maestra debería tener stableId");

        assertEquals(ids1, ids2);
    }

    private Set<String> extractIds(VisualDocument doc) {

        Set<String> ids = new HashSet<>();

        for (VisualBlock block : doc.getBlocks()) {

            if (block instanceof TableBlock tb) {

                TableGrid grid = tb.getGrid();
                CellVisual[][] matrix = grid.getMatrix();

                for (int r = 0; r < grid.getTotalRows(); r++) {
                    for (int c = 0; c < grid.getTotalCols(); c++) {

                        CellVisual cell = matrix[r][c];

                        if (cell == null) continue;
                        if (!cell.isMaster()) continue;

                        ids.add(cell.getStableId());
                    }
                }
            }
        }

        return ids;
    }
}