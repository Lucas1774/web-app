package com.lucas.server.common.controller;

import com.lucas.server.common.dto.user.UserDomain;
import com.lucas.server.common.jpa.user.UserJpaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

    private final ControllerUtil controllerUtil;
    private final UserJpaService userService;

    public AuthenticationController(ControllerUtil controllerUtil, UserJpaService userService) {
        this.controllerUtil = controllerUtil;
        this.userService = userService;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> handleLogin(@RequestBody UserDomain user, HttpServletResponse response) {
        Optional<UserDomain> auth =
                userService.findByUsername(user.getUsername()).filter(u -> u.getPassword().equals(user.getPassword()));
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        response.addCookie(controllerUtil.createCookie("authToken",
                controllerUtil.generateToken(user.getUsername()),
                true,
                31536000));

        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Void> checkAuth(HttpServletRequest request) {
        return controllerUtil.retrieveAuthCookie(request.getCookies())
                .<ResponseEntity<Void>>map(_ -> ResponseEntity.ok().build())
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}
