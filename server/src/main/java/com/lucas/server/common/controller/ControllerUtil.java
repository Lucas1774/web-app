package com.lucas.server.common.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.lucas.server.common.Constants.DEFAULT_USERNAME;

@Component
public class ControllerUtil {

    private static final Logger logger = LoggerFactory.getLogger(ControllerUtil.class);
    private final List<String> admin;
    private final JWTVerifier verifier;
    private final Algorithm algorithm;

    public ControllerUtil(@Value("${spring.security.admin}") List<String> admin, @Value("${spring.security.jwt.secret}") String secretKey) {
        this.admin = admin;
        algorithm = Algorithm.HMAC256(secretKey);
        verifier = JWT.require(algorithm).build();
    }

    public boolean isAdmin(String username) {
        return admin.contains(username);
    }

    public ResponseEntity<String> handleRequest(Callable<String> action) {
        try {
            return ResponseEntity.ok(action.call());
        } catch (Exception e) {
            String message = e.getMessage();
            logger.error(message);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
    }

    public String generateToken(String userName) {
        return JWT.create()
                .withSubject(userName)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365))
                .sign(algorithm);
    }

    public String retrieveUsername(Cookie[] cookies) {
        return retrieveAuthCookie(cookies)
                .map(jwt -> JWT.decode(jwt).getSubject())
                .orElse(DEFAULT_USERNAME);
    }

    public Optional<String> retrieveAuthCookie(Cookie[] cookies) {
        if (null == cookies) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "authToken".equals(cookie.getName()) && isTokenValid(cookie.getValue()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private boolean isTokenValid(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
