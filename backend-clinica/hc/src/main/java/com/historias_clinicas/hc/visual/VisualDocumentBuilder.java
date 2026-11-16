package com.historias_clinicas.hc.visual;

import org.apache.poi.xwpf.usermodel.*;

public class VisualDocumentBuilder {

    private final TableGridBuilder gridBuilder = new TableGridBuilder();
    private static final String BLOCK_PREFIX = "B";

    public VisualDocument build(XWPFDocument doc) {

        VisualDocument visual = new VisualDocument();

        int bodyIndex = 0;
        int tableIndex = 0;
        int visualIndex = 0;

        for (IBodyElement element : doc.getBodyElements()) {

            if (element instanceof XWPFParagraph p) {

                String text = normalize(p.getText());

                if (!text.isEmpty()) {

                    String path = BLOCK_PREFIX + visualIndex;

                    visual.addBlock(
                            new ParagraphBlock(
                                    bodyIndex,
                                    visualIndex,
                                    path,
                                    text
                            )
                    );

                    visualIndex++;
                }

            } else if (element instanceof XWPFTable t) {

                TableGrid grid = gridBuilder.build(t);

                String path = BLOCK_PREFIX + visualIndex;

                visual.addBlock(
                        new TableBlock(
                                bodyIndex,
                                tableIndex++,
                                visualIndex,
                                path,
                                grid,
                                0
                        )
                );

                visualIndex++;
            }

            bodyIndex++;
        }

        return visual;
    }

    private String normalize(String text) {

        if (text == null) return "";

        return text
                .replaceAll("\\s+", " ")
                .trim();
    }
}
