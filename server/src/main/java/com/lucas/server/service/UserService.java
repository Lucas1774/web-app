package com.lucas.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    public static final String DEFAULT = "default";
    @Value("${spring.security.admin}")
    List<String> admin;

    public boolean isAdmin(String username) {
        return this.admin.contains(username);
    }
}