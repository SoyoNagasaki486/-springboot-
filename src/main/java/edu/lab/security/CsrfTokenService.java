package edu.lab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;

/** 基于服务端会话的同步令牌 CSRF 防护。 */
@Component
public class CsrfTokenService {
    public static final String HEADER_NAME = "X-CSRF-TOKEN";
    private static final String SESSION_KEY = "LAB_GUARD_CSRF";
    private final SecureRandom secureRandom = new SecureRandom();

    public String token(HttpSession session) {
        Object current = session.getAttribute(SESSION_KEY);
        if (current instanceof String value && !value.isBlank()) {
            return value;
        }
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, value);
        return value;
    }

    public void validate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(SESSION_KEY);
        String actual = request.getHeader(HEADER_NAME);
        if (expected == null || actual == null || !constantTimeEquals(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "安全令牌无效，请刷新页面后重试");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] left = expected.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(left, right);
    }
}
