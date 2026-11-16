package com.historias_clinicas.hc.visual;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class VisualDocumentSerializer {

    private final ObjectMapper mapper;

    public VisualDocumentSerializer() {

        mapper = new ObjectMapper();

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public String toJson(VisualDocument document) {

        try {

            return mapper.writeValueAsString(document);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error serializando VisualDocument",
                    e
            );
        }
    }
}