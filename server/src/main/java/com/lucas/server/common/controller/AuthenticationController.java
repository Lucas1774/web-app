package com.lucas.server.common.controller;

import com.lucas.server.common.jpa.user.User;
import com.lucas.server.common.jpa.user.UserJpaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.lucas.utils.Utils.EMPTY_STRING;

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
    public ResponseEntity<Void> handleLogin(@RequestBody User user, HttpServletResponse response) {
        Optional<User> auth = userService.findByUsername(user.getUsername())
                .filter(u -> u.getPassword().equals(user.getPassword()));
        if (auth.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Cookie cookie = new Cookie("authToken", controllerUtil.generateToken(user.getUsername()));
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(31536000);
        cookie.setSecure(secure);
        cookie.setAttribute("Partitioned", EMPTY_STRING);
        if (secure) {
            cookie.setAttribute("SameSite", "None");
        }
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Void> checkAuth(HttpServletRequest request) {
        if (controllerUtil.retrieveAuthCookie(request.getCookies()).isPresent()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
