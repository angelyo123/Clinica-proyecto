package com.historias_clinicas.hc.extraction.validation;

import com.historias_clinicas.hc.cognitive.FieldDefinition;
import com.historias_clinicas.hc.extraction.dto.FieldValueAI;
import com.historias_clinicas.hc.extraction.normalizer.FieldValueNormalizer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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

            log.debug("Validando campo: {}", v.getStableId());

            if (def == null) {
                log.warn("❌ Campo inexistente: {}", v.getStableId());
                continue;
            }

            String limpio = FieldValueNormalizer.normalize(
                    v.getValor(),
                    def.getDataType()
            );

            log.debug("Original: [{}] → Limpio: [{}]",
                    v.getValor(),
                    limpio);

            if (limpio == null || limpio.isBlank()) {
                log.warn("❌ Valor vacío tras normalizar: {}", v.getStableId());
                continue;
            }

            if (!validateType(limpio, def.getDataType())) {
                log.warn("❌ Tipo inválido: {} → {}",
                        v.getStableId(),
                        limpio);
                continue;
            }

            log.debug("✅ Campo válido: {} -> {}", v.getStableId(), limpio);

            v.setValor(limpio);

            valid.add(v);
        }

        return valid;
    }

    private static boolean validateType(String value, String tipo) {

        switch (tipo) {

            case "numero":
                return value.matches("\\d+(\\.\\d+)?");

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