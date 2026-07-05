package com.lucas.server.config;

import com.lucas.server.common.controller.ControllerUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SameSiteCsrfTokenRepository implements CsrfTokenRepository {

    private static final String COOKIE_NAME = "XSRF-TOKEN";
    private static final String HEADER_NAME = "X-XSRF-TOKEN";
    private static final String PARAMETER_NAME = "_csrf";
    private final ControllerUtil controllerUtil;

    @NonNull
    @Override
    public CsrfToken generateToken(@NonNull HttpServletRequest request) {
        return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, UUID.randomUUID().toString());
    }

    @Override
    public void saveToken(CsrfToken token, @NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        String tokenValue = null != token ? token.getToken() : "";
        int maxAage = null != token ? -1 : 0;
        response.addCookie(controllerUtil.createCookie(COOKIE_NAME, tokenValue, false, maxAage));
    }

    @Override
    public CsrfToken loadToken(@NonNull HttpServletRequest request) {
        Cookie cookie = getCookie(request);
        if (null == cookie || !StringUtils.hasLength(cookie.getValue())) {
            return null;
        }
        return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, cookie.getValue());
    }

    private Cookie getCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (null == cookies) {
            return null;
        }
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }
}
