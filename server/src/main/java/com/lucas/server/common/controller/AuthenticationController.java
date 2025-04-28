package com.lucas.server.common.controller;

import com.lucas.server.common.jpa.user.User;
import com.lucas.server.common.jpa.user.UserJpaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

    private final ControllerUtil controllerUtil;
    private final UserJpaService userService;
    private final boolean secure;

    public AuthenticationController(ControllerUtil controllerUtil, UserJpaService userService, @Value("${spring.security.jwt.secure}") boolean secure) {
        this.controllerUtil = controllerUtil;
        this.userService = userService;
        this.secure = secure;
    }

    @PostMapping("/login")
    public ResponseEntity<String> handleLogin(@RequestBody User user, HttpServletResponse response) {
        return userService.findByUsername(user.getUsername())
                .filter(u -> u.getPassword().equals(user.getPassword().replace("\"", "")))
                .map(u -> {
                    String token = this.controllerUtil.generateToken(u.getUsername());
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
                })
                .orElseGet(() ->
                        ResponseEntity.ok("Wrong credentials. Continuing as guest")
                );
    }

    @GetMapping("/check-auth")
    public ResponseEntity<String> checkAuth(HttpServletRequest request) {
        return this.controllerUtil.handleRequest(() -> this.controllerUtil.retrieveAuthCookie(request.getCookies()).isPresent()
                ? "1"
                : "Not authenticated");
    }
}
