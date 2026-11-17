package edu.lab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/** API 登录拦截器，业务角色权限由控制层继续细分。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String REQUEST_ID_ATTRIBUTE = "LAB_GUARD_REQUEST_ID";
    private static final Set<String> PUBLIC_PATHS = Set.of("/api/login", "/api/register", "/api/session");
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final CsrfTokenService csrfTokenService;

    public AuthInterceptor(CsrfTokenService csrfTokenService) {
        this.csrfTokenService = csrfTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        addSecurityHeaders(response);
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || !requestId.matches("[A-Za-z0-9_-]{8,64}")) {
            requestId = UUID.randomUUID().toString();
        }
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader("X-Request-ID", requestId);
        if (PUBLIC_PATHS.contains(request.getRequestURI())) {
            return true;
        }
        Object user = request.getSession(false) == null
                ? null : request.getSession(false).getAttribute(CurrentUserService.SESSION_KEY);
        if (!(user instanceof SessionUser)) {
            unauthorized(request, response);
            return false;
        }
        if (!SAFE_METHODS.contains(request.getMethod())) {
            try {
                csrfTokenService.validate(request);
            } catch (ResponseStatusException exception) {
                forbidden(request, response, exception.getReason());
                return false;
            }
        }
        return true;
    }

    private void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cache-Control", "no-store");
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        writeJson(request, response, "AUTH_REQUIRED", "请先登录后再操作");
    }

    private void forbidden(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        writeJson(request, response, "CSRF_INVALID", message);
    }

    private void writeJson(
            HttpServletRequest request, HttpServletResponse response, String code, String message) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"status\":" + response.getStatus()
                + ",\"message\":\"" + message + "\",\"path\":\"" + request.getRequestURI()
                + "\",\"request_id\":\"" + request.getAttribute(REQUEST_ID_ATTRIBUTE) + "\"}");
    }
}
