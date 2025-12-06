package com.historias_clinicas.hc.servicios.word;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class WordRenderEngine {

    public void escribirCeldaCompleta(
            XWPFTableCell celda,
            Map<String, Object> instruccion
    ) {
        try {

            String estrategia = (String) instruccion.get("strategy");

            if ("rewrite".equals(estrategia)) {
                escribirTextoPlano(celda, (String) instruccion.get("textoFinal"));
                return;
            }

            if ("texto_plano".equals(estrategia)) {
                escribirTextoPlano(celda, (String) instruccion.get("textoFinal"));
                return;
            }
            if (estrategia == null) {
                log.warn("⚠️ IA sin estrategia, colocando texto vacío");
                escribirTextoPlano(celda, "");
                return;
            }


        } catch (Exception e) {
            log.error("❌ Error escribiendo celda", e);
        }
    }

    private void escribirTextoPlano(XWPFTableCell celda, String texto) {

        if (celda.getParagraphs().isEmpty())
            celda.addParagraph();

        XWPFParagraph p = celda.getParagraphs().get(0);

        // Borrar runs
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }

        XWPFRun r = p.createRun();
        r.setText(texto == null ? "" : texto);
    }
}
