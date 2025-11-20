package com.historias_clinicas.hc.ia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ImageInterpreter {

    private final DeepSeekClient deepSeekClient;

    public String leerImagen(MultipartFile file) throws Exception {

        // 1. Convertir imagen a base64
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());

        // 2. Prompt para DeepSeek Vision
        String prompt = """
                Eres un médico experto capaz de analizar imágenes de historias clínicas manuscritas o impresas.

                Te enviaré una imagen codificada en base64.
                Extrae TODO el texto de forma clara, sin inventar nada.
                Devuelve SOLO el texto plano, sin JSON.

                Imagen codificada:
                data:%s;base64,%s
                """
                .formatted(file.getContentType(), base64);

        // 3. DeepSeek devuelve texto
        return deepSeekClient.completar(prompt);
    }
}