package com.historias_clinicas.hc.servicios;

import com.historias_clinicas.hc.entidades.Plantilla;
import com.historias_clinicas.hc.entidades.PlantillaCampo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordFillService {

    /**
     * Genera un Word final rellenado usando:
     * - La plantilla original (Plantilla.archivoOriginal)
     * - Los valores confirmados por el médico
     * - Las posiciones exactas detectadas por PlantillaProcessorService
     */
    public byte[] generarWordRelleno(Plantilla plantilla, Map<PlantillaCampo, String> valores) {

        try (XWPFDocument doc = new XWPFDocument(
                new ByteArrayInputStream(plantilla.getArchivoOriginal()))) {

            for (var entry : valores.entrySet()) {
                PlantillaCampo campo = entry.getKey();
                String valor = entry.getValue();
                insertarValor(doc, campo, valor);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar Word final: " + e.getMessage(), e);
        }
    }

    /**
     * Inserta un valor EXACTAMENTE en el run donde estaba la línea/espacio.
     * Mantiene estilo, tabla, margen, diseño.
     */
    private void insertarValor(XWPFDocument doc, PlantillaCampo campo, String valor) {

        try {
            // Caso: campo en tabla
            if (campo.getIndexTabla() != null) {
                XWPFTable tabla = doc.getTables().get(campo.getIndexTabla());
                XWPFTableRow fila = tabla.getRow(campo.getIndexFila());
                XWPFTableCell celda = fila.getCell(campo.getIndexCelda());
                XWPFParagraph parrafo = celda.getParagraphs().get(0);

                XWPFRun run = parrafo.getRuns().get(campo.getIndexRunInicio());
                run.setText(valor, 0);
                return;
            }

            // Caso: campo en párrafo normal
            XWPFParagraph parrafo = doc.getParagraphs().get(campo.getIndexParrafo());
            XWPFRun run = parrafo.getRuns().get(campo.getIndexRunInicio());
            run.setText(valor, 0);

        } catch (Exception ex) {
            log.error("Error insertando valor '{}' en campo {}: {}", valor, campo.getNombreCampo(), ex.getMessage());
        }
    }
}