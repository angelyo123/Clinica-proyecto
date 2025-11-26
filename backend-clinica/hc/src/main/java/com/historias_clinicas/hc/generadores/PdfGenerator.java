package com.historias_clinicas.hc.generadores;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class PdfGenerator {

    private LocalOfficeManager officeManager;

    @PostConstruct
    public void init() {
        try {
            System.out.println(">>> Iniciando LibreOffice...");
            officeManager = LocalOfficeManager.builder()
                    .install()
                    .build();

            officeManager.start();
            System.out.println(">>> LibreOffice iniciado correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("LibreOffice no pudo iniciar: " + e.getMessage());
        }
    }


    @PreDestroy
    public void shutdown() {
        try {
            if (officeManager != null) {
                officeManager.stop();
            }
        } catch (Exception ignored) {}
    }

    public byte[] convertToPdf(byte[] wordBytes) {
        try {

            DocumentConverter converter = LocalConverter.make(officeManager);

            ByteArrayInputStream in = new ByteArrayInputStream(wordBytes);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            converter.convert(in)
                    .as(DefaultDocumentFormatRegistry.DOCX)
                    .to(out)
                    .as(DefaultDocumentFormatRegistry.PDF)
                    .execute();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error convirtiendo Word a PDF: " + e.getMessage(), e);
        }
    }
}
