package com.AutomatizacionService.service.conversacionIA;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversacionUtils {

    private static final Map<String, String> mapaPalabras = new HashMap<>();

    static {
        // ❤️ Cardiología
        mapaPalabras.put("corazon", "Cardiología");
        mapaPalabras.put("pecho", "Cardiología");
        mapaPalabras.put("presion", "Cardiología");
        mapaPalabras.put("palpitacion", "Cardiología");
        mapaPalabras.put("hipertension", "Cardiología");

        // 🧠 Neurología
        mapaPalabras.put("cabeza", "Neurología");
        mapaPalabras.put("mareo", "Neurología");
        mapaPalabras.put("migraña", "Neurología");
        mapaPalabras.put("nervio", "Neurología");
        mapaPalabras.put("temblor", "Neurología");

        // 🦴 Traumatología
        mapaPalabras.put("hueso", "Traumatología");
        mapaPalabras.put("rodilla", "Traumatología");
        mapaPalabras.put("hombro", "Traumatología");
        mapaPalabras.put("columna", "Traumatología");
        mapaPalabras.put("fractura", "Traumatología");

        // 👶 Pediatría
        mapaPalabras.put("niño", "Pediatría");
        mapaPalabras.put("bebe", "Pediatría");
        mapaPalabras.put("fiebre infantil", "Pediatría");
        mapaPalabras.put("menor", "Pediatría");

        // 🩺 Medicina general
        mapaPalabras.put("malestar", "Medicina General");
        mapaPalabras.put("dolor general", "Medicina General");
        mapaPalabras.put("consulta general", "Medicina General");

        // 🧴 Dermatología
        mapaPalabras.put("piel", "Dermatología");
        mapaPalabras.put("mancha", "Dermatología");
        mapaPalabras.put("acne", "Dermatología");
        mapaPalabras.put("sarpullido", "Dermatología");
        mapaPalabras.put("picazon", "Dermatología");

        // 👁️ Oftalmología (❗️sin "ver", es demasiado genérica)
        mapaPalabras.put("ojo", "Oftalmología");
        mapaPalabras.put("vision", "Oftalmología");
        mapaPalabras.put("ardor ocular", "Oftalmología");
        mapaPalabras.put("vista borrosa", "Oftalmología");

        // 🦷 Odontología
        mapaPalabras.put("diente", "Odontología");
        mapaPalabras.put("muela", "Odontología");
        mapaPalabras.put("encia", "Odontología");

        // 👂 Otorrinolaringología
        mapaPalabras.put("oido", "Otorrinolaringología");
        mapaPalabras.put("nariz", "Otorrinolaringología");
        mapaPalabras.put("garganta", "Otorrinolaringología");
    }

    /** Detección por nombre directo de especialidad (cardiologia, derma, oftalmo, etc.) */
    public static String detectarEspecialidadPorNombre(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) return null;
        String t = normalizar(mensaje);
        if (t.contains("cardiolog")) return "Cardiología";
        if (t.contains("dermatolog") || t.contains("derma")) return "Dermatología";
        if (t.contains("pediatr")) return "Pediatría";
        if (t.contains("neurolog")) return "Neurología";
        if (t.contains("traumatolog") || t.contains("trauma")) return "Traumatología";
        if (t.contains("medicina general") || (t.contains("medicina") && t.contains("general"))) return "Medicina General";
        if (t.contains("oftalmolog") || t.contains("oftalmo")) return "Oftalmología";
        if (t.contains("odontolog") || t.contains("dental")) return "Odontología";
        if (t.contains("otorrino") || t.contains("otorrinolaringolog")) return "Otorrinolaringología";
        return null;
    }

    public static String deducirEspecialidad(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) return null;
        String limpio = normalizar(mensaje);

        for (Map.Entry<String, String> entry : mapaPalabras.entrySet()) {
            if (limpio.contains(entry.getKey())) {
                System.out.println("🧠 Deducción: palabra '" + entry.getKey() +
                        "' → especialidad " + entry.getValue());
                return entry.getValue();
            }
        }
        return null;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .trim();
    }

    public static String extraerHoraDesdeTexto(String texto) {
        if (texto == null) return null;
        // Detecta patrones como "a las 14", "14:00", "2 pm", "a las 9 de la mañana"
        Pattern p = Pattern.compile("(\\b\\d{1,2})([:hH]?(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            int hora = Integer.parseInt(m.group(1));
            String minutos = (m.group(3) != null) ? m.group(3) : "00";
            String ampm = (m.group(4) != null) ? m.group(4).toLowerCase() : "";

            if (ampm.equals("pm") && hora < 12) hora += 12;
            if (ampm.equals("am") && hora == 12) hora = 0;

            return String.format("%02d:%s", hora, minutos);
        }
        return null;
    }
}
