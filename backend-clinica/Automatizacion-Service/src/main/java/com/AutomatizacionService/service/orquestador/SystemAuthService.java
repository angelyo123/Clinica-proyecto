package com.AutomatizacionService.service.orquestador;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * Servicio que maneja la autenticación del usuario "soy la IA".
 * - Obtiene el token JWT desde el Auth-Service.
 * - Lo almacena en memoria (cache).
 * - Detecta si el token expira y lo renueva automáticamente.
 */
@Component
public class SystemAuthService {

    @Value("${auth.service.url:http://localhost:8081/auth/login}")
    private String authUrl;

    @Value("${ia.username:soy la IA}")
    private String username;

    @Value("${ia.password:1234}")
    private String password;

    @Value("${jwt.secret:SecretKeyForJwtGenerationSuperSeguraDeAlMenos32Chars}")
    private String jwtSecret;

    private String cachedToken;
    private Date expirationDate;

    private final RestTemplate restTemplate = new RestTemplate();

    /** Obtiene el token de sistema (renueva si expiró). */
    public synchronized String getSystemToken() {
        if (cachedToken == null || isTokenExpired()) {
            authenticateSystemUser();
        }
        return cachedToken;
    }

    /** Realiza login contra el Auth-Service y guarda el token. */
    private void authenticateSystemUser() {
        System.out.println("🤖 [IA] Autenticando usuario del sistema: " + username);
        Map<String, String> body = Map.of("username", username, "password", password);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(authUrl, body, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                cachedToken = (String) response.getBody().get("token");
                expirationDate = extractExpiration(cachedToken);
                System.out.println("✅ [IA] Token obtenido correctamente. Expira: " + expirationDate);
            } else {
                throw new RuntimeException("Error al autenticarse. Código: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("❌ Error al autenticar al usuario IA: " + e.getMessage(), e);
        }
    }

    /** Verifica si el token actual está expirado. */
    private boolean isTokenExpired() {
        if (expirationDate == null) return true;
        Date now = new Date();
        // Consideramos margen de 1 minuto antes de expiración
        return now.after(new Date(expirationDate.getTime() - 60_000));
    }

    /** Extrae la fecha de expiración desde el JWT. */
    private Date extractExpiration(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            System.out.println("⚠️ [IA] No se pudo extraer fecha de expiración del token.");
            return new Date(0);
        }
    }
}