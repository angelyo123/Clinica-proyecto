package com.historias_clinicas.hc.extraction.mapper;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.dto.CampoValorConfirmado;
import com.historias_clinicas.hc.extraction.dto.FieldValueAI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldValueMapper {

    public static List<CampoValorConfirmado> map(
            List<FieldValueAI> values,
            List<FieldDefinition> defs) {

        Map<String, FieldDefinition> map =
                defs.stream().collect(Collectors.toMap(
                        FieldDefinition::getStableId,
                        f -> f
                ));

        List<CampoValorConfirmado> result = new ArrayList<>();

        for (FieldValueAI v : values) {

            FieldDefinition def = map.get(v.getStableId());

            CampoValorConfirmado c = new CampoValorConfirmado();

            c.setStableId(def.getWriteStableId());
            c.setValor(v.getValor());
            c.setTipoDato(def.getDataType());

            System.out.println("MAP: IA " + v.getStableId());
            System.out.println("MAP: DEF " + def);
            result.add(c);
        }

        return result;
    }
}