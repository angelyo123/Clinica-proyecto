package com.authservice.security;

import com.authservice.client.PacienteClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "SecretKeyForJwtGenerationSuperSeguraDeAlMenos32Chars";
    private final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 hora

    @Autowired
    private PacienteClient pacienteClient;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /** 🔹 Generar token con roles incluidos **/
    public String generateToken(Authentication authentication, Long userId) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userPrincipal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("id", userId); // 👈 incluimos el ID del usuario

        // 🔹 Si el usuario es paciente, obtén su ID real desde el microservicio
        if (userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"))) {

            try {
                var paciente = pacienteClient.obtenerPorUsername(userPrincipal.getUsername());
                if (paciente != null && paciente.getId() != null) {
                    claims.put("pacienteId", paciente.getId());
                }
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo obtener el paciente desde el microservicio: " + e.getMessage());
            }
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 🔹 Extraer username del token **/
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** 🔹 Extraer roles del token **/
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 🔹 Validar token **/
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
