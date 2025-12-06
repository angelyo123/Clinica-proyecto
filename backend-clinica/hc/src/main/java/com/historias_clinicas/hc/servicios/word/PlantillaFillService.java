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

    private final AiRenderService ai;
    private final WordRenderEngine word;

    public byte[] llenarWord(
            byte[] plantillaBytes,
            List<CampoValorConfirmado> valores,
            List<PlantillaCampo> campos
    ) throws Exception {

        XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(plantillaBytes));

        // Agrupar por celda
        Map<String, List<CampoValorConfirmado>> mapa = new LinkedHashMap<>();

        for (CampoValorConfirmado v : valores) {
            PlantillaCampo c = buscar(v.getCampoId(), campos);
            if (c == null) continue;

            mapa.computeIfAbsent(keyCelda(c), k -> new ArrayList<>()).add(v);
        }

        // Procesar cada celda
        for (String key : mapa.keySet()) {

            List<CampoValorConfirmado> listaValores = mapa.get(key);
            PlantillaCampo ref = buscar(listaValores.get(0).getCampoId(), campos);

            // Obtener la celda real del Word
            XWPFTableCell celda = doc.getTableArray(ref.getIndexTabla())
                    .getRow(ref.getIndexFila())
                    .getCell(ref.getIndexCelda());

            if (celda == null) continue;

            String textoCeldaReal = extraerTextoCelda(celda);

            // Obtener todos los campos pertenecientes a esta celda
            List<PlantillaCampo> listaCampos = campos.stream()
                    .filter(c ->
                            Objects.equals(c.getIndexTabla(), ref.getIndexTabla()) &&
                                    Objects.equals(c.getIndexFila(), ref.getIndexFila()) &&
                                    Objects.equals(c.getIndexCelda(), ref.getIndexCelda())
                    )
                    .sorted(Comparator.comparing(PlantillaCampo::getItemIndex))
                    .toList();

            List<Map<String, Object>> items = new ArrayList<>();

            for (PlantillaCampo c : listaCampos) {

                String valor = listaValores.stream()
                        .filter(v -> v.getCampoId().equals(c.getId()))
                        .map(CampoValorConfirmado::getValor)
                        .findFirst()
                        .orElse(null);

                Map<String, Object> it = new LinkedHashMap<>();
                it.put("descripcion", c.getDescripcionCampo());
                it.put("tipo", c.getTipoCampo());
                it.put("valor", valor);
                it.put("itemIndex", c.getItemIndex());

                items.add(it);
            }

            // Si es una celda_llenable simple (como PULSOS), aplicar shortcut:
            if (items.size() == 1 && "celda_llenable".equals(items.get(0).get("tipo"))) {

                String valor = (String) items.get(0).get("valor");

                Map<String, Object> instruccionDirecta = new HashMap<>();
                instruccionDirecta.put("strategy", "texto_plano");
                instruccionDirecta.put("textoFinal", valor != null ? valor : "");

                word.escribirCeldaCompleta(celda, instruccionDirecta);
                continue; // ← evitar IA
            }

            // → Llamar IA
            Map<String, Object> instruccion = ai.generarInstruccionCelda(textoCeldaReal, items);

            // → Escribir en Word
            word.escribirCeldaCompleta(celda, instruccion);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }


    private String extraerTextoCelda(XWPFTableCell celda) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : celda.getParagraphs()) {
            sb.append(p.getText()).append(" ");
        }
        return sb.toString().trim();
    }

    private PlantillaCampo buscar(Long id, List<PlantillaCampo> lista) {
        return lista.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private String keyCelda(PlantillaCampo c) {
        return c.getIndexTabla() + "-" + c.getIndexFila() + "-" + c.getIndexCelda();
    }
}