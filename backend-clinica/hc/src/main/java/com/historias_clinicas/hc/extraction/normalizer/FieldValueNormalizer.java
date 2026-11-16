package com.historias_clinicas.hc.extraction.normalizer;

public class FieldValueNormalizer {

    public static String normalize(String value, String tipo) {

        if (value == null) return null;

        value = value.trim();

        switch (tipo) {

            case "numero":
                return value.replaceAll("[^0-9.]", "");

            case "fecha":
                return normalizeFecha(value);

            case "booleano":
                return normalizeBoolean(value);

            default:
                return value;
        }
    }

    private static String normalizeBoolean(String value) {

        value = value.toLowerCase();

        if (value.contains("si") || value.equals("1") || value.equals("x"))
            return "true";

        if (value.contains("no") || value.equals("0"))
            return "false";

        return value;
    }

    private static String normalizeFecha(String value) {

        // básico por ahora (puedes mejorar luego)
        return value.replace("/", "-");
    }
}