package com.seegeneroso.gestao_custos_obras.shared.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class JwtService {

    @Value("${app.jwt.secret:changeme-256-bits-min-32-chars-secret-key-dev-only}")
    private String secret;

    @Value("${app.jwt.expiration-minutos:120}")
    private long expiracaoMinutos;

    private SecretKey chave() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // O token carrega só a identidade (e-mail) — a autorização não viaja nele: é resolvida a
    // cada requisição contra a matriz de permissões (ADR-046), pra uma mudança de perfil feita
    // em runtime valer imediatamente, sem esperar o token expirar.
    public String gerarToken(String email) {
        Date agora = new Date();
        Date expira = new Date(agora.getTime() + Duration.ofMinutes(expiracaoMinutos).toMillis());
        return Jwts.builder()
                .subject(email)
                .issuedAt(agora)
                .expiration(expira)
                .signWith(chave())
                .compact();
    }

    public String extrairEmail(String token) {
        return parse(token).getSubject();
    }

    public boolean valido(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
