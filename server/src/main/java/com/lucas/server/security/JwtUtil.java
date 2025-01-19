package com.lucas.server.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtUtil {
    private final JWTVerifier verifier;
    private final Algorithm algorithm;

    public JwtUtil(@Value("${spring.security.jwt.secret}") String secretKey) {
        this.algorithm = Algorithm.HMAC256(secretKey);
        this.verifier = JWT.require(this.algorithm).build();
    }

    public String generateToken(String userName) {
        return JWT.create()
                .withSubject(userName)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365))
                .sign(algorithm);
    }

    public boolean isTokenValid(String token) {
        try {
            this.verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
