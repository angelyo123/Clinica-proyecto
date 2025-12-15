package com.historias_clinicas.hc.servicios.word;

import lombok.RequiredArgsConstructor;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.local.LocalConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class WordToPdfService {

    private final LocalConverter converter;

    public byte[] convertirWordAPdf(byte[] wordBytes) {

        try (
                ByteArrayInputStream in = new ByteArrayInputStream(wordBytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            converter.convert(in)
                    .as(DefaultDocumentFormatRegistry.DOCX)
                    .to(out)
                    .as(DefaultDocumentFormatRegistry.PDF)
                    .execute();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error convirtiendo Word a PDF", e);
        }
    }
}