package com.historias_clinicas.hc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historias_clinicas.hc.cognitive.CognitiveSerializer;
import com.historias_clinicas.hc.dto.CognitiveCell;
import com.historias_clinicas.hc.visual.*;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.FileInputStream;
import java.util.List;

public class TestCognitiveSerializer {

    public static void main(String[] args) throws Exception {

        // 1️⃣ abrir DOCX
        XWPFDocument doc = new XWPFDocument(
                TestCognitiveSerializer.class
                        .getClassLoader()
                        .getResourceAsStream("historia_clinica.docx")
        );

        // 2️⃣ construir modelo visual
        VisualDocumentBuilder builder = new VisualDocumentBuilder();
        VisualDocument visual = builder.build(doc);

        // 3️⃣ generar stable IDs
        StableIdGenerator generator = new StableIdGenerator();
        generator.generarIds(visual);

        // 4️⃣ serializar cognitivamente
        CognitiveSerializer serializer = new CognitiveSerializer();
        List<CognitiveCell> cells = serializer.serialize(visual);

        // 5️⃣ imprimir JSON
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(cells);

        System.out.println(json);

        // 6️⃣ mostrar primeras 10 celdas
        System.out.println("---- PRIMERAS CELDAS ----");

        cells.stream()
                .limit(10)
                .forEach(c -> System.out.println(
                        c.getStableId()
                                + " span=" + c.getRowSpan()
                                + "x" + c.getColSpan()
                                + " depth=" + c.getDepth()
                ));
    }
}