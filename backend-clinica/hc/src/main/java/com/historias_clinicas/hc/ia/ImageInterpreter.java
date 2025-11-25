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

        String base64 = Base64.getEncoder().encodeToString(file.getBytes());

        String instrucciones = """
            Extrae TODO el texto visible en la imagen.
            No inventes información.
            No completes huecos.
            Devuelve únicamente texto plano.
            """;

        return deepSeekClient.completarImagen(base64, instrucciones);
    }
}