package com.lucas.server.common.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.lucas.server.common.Constants.DEFAULT_USERNAME;

@Component
@Slf4j
public class ControllerUtil {

    private final Set<String> admin;
    private final JWTVerifier verifier;
    private final Algorithm algorithm;
    private final boolean secure;

    public ControllerUtil(@Value("${spring.security.admin}") Set<String> admin,
                          @Value("${spring.security.jwt.secret}") String secretKey,
                          @Value("${spring.security.jwt.secure}") boolean secure) {
        this.admin = admin;
        algorithm = Algorithm.HMAC256(secretKey);
        verifier = JWT.require(algorithm).build();
        this.secure = secure;
    }

    public boolean isAdmin(String username) {
        return admin.contains(username);
    }

    public ResponseEntity<String> handleRequest(Callable<String> action) {
        try {
            return ResponseEntity.ok(action.call());
        } catch (Exception e) {
            String message = e.getMessage();
            log.error(message);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
    }

    public String generateToken(String userName) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(userName)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(365, ChronoUnit.DAYS))
                .sign(algorithm);
    }

    public Cookie createCookie(String name, String value, boolean httpOnly, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);

        if (secure) {
            cookie.setAttribute("Partitioned", "");
            cookie.setAttribute("SameSite", "None");
        }

        return cookie;
    }

    public <T> Optional<ResponseEntity<T>> getUnauthorizedResponseIfInvalidUser(Cookie[] cookies) {
        String username = retrieveUsername(cookies);
        if (DEFAULT_USERNAME.equals(username)) {
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return Optional.empty();
    }

    public String retrieveUsername(Cookie[] cookies) {
        return retrieveAuthCookie(cookies).map(jwt -> JWT.decode(jwt).getSubject()).orElse(DEFAULT_USERNAME);
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
        } catch (Exception _) {
            return false;
        }
    }
}
