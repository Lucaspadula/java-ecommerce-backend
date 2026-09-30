package com.sistventas.backend.security;

import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida los JWT de sesión. El secret y el tiempo de expiración
 * son configurables vía sistventas.jwt.secret / sistventas.jwt.expiration-ms
 * en application.yml.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${sistventas.jwt.secret}") String secret,
            @Value("${sistventas.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Usuario usuario) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("esSuperAdmin", usuario.isEsSuperAdmin())
                .issuedAt(now)
                .expiration(expiry);

        if (usuario.getEmpresa() != null) {
            builder.claim("empresaId", usuario.getEmpresa().getId());
        }
        if (usuario.getRolEmpresa() != null) {
            builder.claim("rolEmpresa", usuario.getRolEmpresa().name());
        }

        return builder.signWith(key).compact();
    }

    // Token de CUENTA DE CLIENTE (login en la tienda pública) — claim "tipo"
    // explícito ("cliente") es lo que le permite a JwtAuthenticationFilter
    // distinguirlo de un token de Usuario (panel/empresa), que nunca tiene
    // ese claim. Nunca lleva rolEmpresa/esSuperAdmin: un cliente no tiene
    // ningún rol de empresa.
    public String generateTokenCliente(Cliente cliente) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(cliente.getId()))
                .claim("tipo", "cliente")
                .claim("empresaId", cliente.getEmpresaId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
