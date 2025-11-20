package com.historias_clinicas.hc.generadores;

import com.historias_clinicas.hc.entidades.CampoValor;
import com.historias_clinicas.hc.entidades.HistoriaClinicaVersion;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import com.historias_clinicas.hc.repositorios.CampoValorRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordGenerator {

    /**
     * Generar documento Word reemplazando todos los campos detectados.
     */
    public byte[] generarDocumento(byte[] plantillaBytes,
                                   Map<PlantillaCampo, String> valores) throws Exception {

        XWPFDocument doc = new XWPFDocument(
                new java.io.ByteArrayInputStream(plantillaBytes)
        );

        for (Map.Entry<PlantillaCampo, String> entry : valores.entrySet()) {
            insertarValorEnDocumento(doc, entry.getKey(), entry.getValue());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }

    // ==========================================================================
    // Insertar valor según posición exacta (tabla o párrafo normal)
    // ==========================================================================
    private void insertarValorEnDocumento(XWPFDocument doc, PlantillaCampo campo, String valor) {

        try {

            XWPFParagraph p;

            if (campo.getIndexTabla() == null) {
                // ---- Caso 1: Está fuera de tablas ----
                p = doc.getParagraphArray(campo.getIndexParrafo());
            } else {
                // ---- Caso 2: Está dentro de una tabla ----
                XWPFTable tabla = doc.getTables().get(campo.getIndexTabla());
                XWPFTableRow fila = tabla.getRow(campo.getIndexFila());
                XWPFTableCell celda = fila.getCell(campo.getIndexCelda());

                if (celda.getParagraphs().isEmpty()) return;

                p = celda.getParagraphArray(0);
            }

            if (p == null || p.getRuns().isEmpty()) return;

            insertarValorEnParrafo(p, campo, valor);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================================================
    // Insertar valor al lado del run de la etiqueta sin romper formato
    // ==========================================================================
    private void insertarValorEnParrafo(XWPFParagraph p, PlantillaCampo campo, String valor) {

        int idxRun = campo.getIndexRunFin();

        if (idxRun >= p.getRuns().size()) return;

        XWPFRun runEtiqueta = p.getRuns().get(idxRun);

        // Crear nuevo run para el valor
        XWPFRun runValor = p.insertNewRun(idxRun + 1);

        // Copiar formato
        if (runEtiqueta.getFontFamily() != null)
            runValor.setFontFamily(runEtiqueta.getFontFamily());

        if (runEtiqueta.getFontSize() > 0)
            runValor.setFontSize(runEtiqueta.getFontSize());

        runValor.setBold(runEtiqueta.isBold());
        runValor.setItalic(runEtiqueta.isItalic());
        runValor.setColor(runEtiqueta.getColor());

        // Insertar texto
        runValor.setText(" " + valor);
    }
}