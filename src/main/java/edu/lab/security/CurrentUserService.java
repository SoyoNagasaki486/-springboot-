package edu.lab.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

/** 统一读取会话身份并执行角色校验。 */
@Component
public class CurrentUserService {
    public static final String SESSION_KEY = "LAB_GUARD_USER";

    public SessionUser require(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof SessionUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录");
    }

    public SessionUser requireRole(HttpSession session, String... roles) {
        SessionUser user = require(session);
        if (Arrays.stream(roles).noneMatch(user.role()::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权执行此操作");
        }
        return user;
    }
}
