package com.historias_clinicas.hc.servicios.word;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfToPngService {

    public List<byte[]> convertirTodasLasPaginasAPng(byte[] pdfBytes) {

        try (PDDocument doc = PDDocument.load(pdfBytes)) {

            PDFRenderer renderer = new PDFRenderer(doc);
            List<byte[]> imagenes = new ArrayList<>();

            for (int i = 0; i < doc.getNumberOfPages(); i++) {

                BufferedImage image = renderer.renderImageWithDPI(
                        i,
                        200
                );

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                imagenes.add(out.toByteArray());
            }

            return imagenes;

        } catch (Exception e) {
            throw new RuntimeException("Error convirtiendo PDF a PNGs", e);
        }
    }
}