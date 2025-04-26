package com.lucas.server.common.controller;

import com.lucas.server.common.dto.User;
import com.lucas.server.connection.DAO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

    private final ControllerUtil controllerUtil;
    private final DAO dao;
    private final boolean secure;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    public AuthenticationController(ControllerUtil controllerUtil, DAO dao, @Value("${spring.security.jwt.secure}") boolean secure) {
        this.controllerUtil = controllerUtil;
        this.dao = dao;
        this.secure = secure;
    }

    @PostMapping("/login")
    public ResponseEntity<String> handleLogin(@RequestBody User user, HttpServletResponse response) {
        Optional<String> correctPassword;
        String username = user.getUsername();
        try {
            correctPassword = dao.getPassword(username);
        } catch (DataAccessException e) {
            String message = e.getMessage();
            logger.error(message);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
        if (correctPassword.isEmpty() || !correctPassword.get().equals(user.getPassword().replace("\"", ""))) {
            return ResponseEntity.ok().body("Wrong credentials. Continuing as guest");
        } else {
            String token = this.controllerUtil.generateToken(username);
            Cookie cookie = new Cookie("authToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(31536000);
            cookie.setSecure(this.secure);
            cookie.setAttribute("Partitioned", "");
            if (this.secure) {
                cookie.setAttribute("SameSite", "None");
            }
            response.addCookie(cookie);
            return ResponseEntity.ok("Granted access");
        }
    }

    @GetMapping("/check-auth")
    public ResponseEntity<String> checkAuth(HttpServletRequest request) {
        return this.controllerUtil.handleRequest(() -> this.controllerUtil.retrieveAuthCookie(request.getCookies()).isPresent()
                ? "1"
                : "Not authenticated");
    }
}
