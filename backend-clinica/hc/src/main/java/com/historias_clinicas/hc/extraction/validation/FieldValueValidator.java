package com.historias_clinicas.hc.extraction;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.extraction.dto.FieldValueAI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldValueValidator {

    public static List<FieldValueAI> validate(
            List<FieldValueAI> values,
            List<FieldDefinition> fields) {

        Map<String, FieldDefinition> map =
                fields.stream()
                        .collect(Collectors.toMap(
                                FieldDefinition::getStableId,
                                f -> f
                        ));

        List<FieldValueAI> valid = new ArrayList<>();

        for (FieldValueAI v : values) {

            FieldDefinition def = map.get(v.getStableId());

            if (def == null)
                continue;

            if (!validateType(v.getValor(), def.getTipoDato()))
                continue;

            valid.add(v);
        }

        return valid;
    }

    private static boolean validateType(String value, String tipo) {

        switch (tipo) {

            case "numero":
                return value.matches("\\d+");

            case "fecha":
                return value.matches("\\d{4}-\\d{2}-\\d{2}");

            case "booleano":
                return value.equalsIgnoreCase("true")
                        || value.equalsIgnoreCase("false");

            default:
                return true;
        }
    }
}