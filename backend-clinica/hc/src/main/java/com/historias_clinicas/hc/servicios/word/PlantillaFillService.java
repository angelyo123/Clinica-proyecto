package com.historias_clinicas.hc.servicios.word;

import com.historias_clinicas.hc.dto.CampoValorConfirmado;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

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
            // ===============================
            if (v.getIndexTabla() != null &&
                    v.getIndexFila()  != null &&
                    v.getIndexCelda() != null) {

                XWPFTable tabla = doc.getTableArray(v.getIndexTabla());
                if (tabla == null) continue;

                XWPFTableRow fila = tabla.getRow(v.getIndexFila());
                if (fila == null) continue;

                XWPFTableCell celda = fila.getCell(v.getIndexCelda());
                if (celda == null) continue;

                limpiarCelda(celda);
                escribirCelda(celda, v.getValor());
            }

            // ===============================
            // CASO 2: PÁRRAFO
            // ===============================
            else if (v.getIndexParrafo() != null) {

                XWPFParagraph p = doc.getParagraphArray(v.getIndexParrafo());
                if (p == null) continue;

                p.getRuns().clear();
                p.createRun().setText(
                        v.getValor() != null ? v.getValor() : ""
                );
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }

    // ===============================
    // HELPERS
    // ===============================
    private void limpiarCelda(XWPFTableCell cell) {
        cell.removeParagraph(0);
        cell.addParagraph().createRun().setText("");
    }

    private void escribirCelda(XWPFTableCell cell, String texto) {
        cell.removeParagraph(0);
        cell.addParagraph().createRun()
                .setText(texto != null ? texto : "");
    }
}