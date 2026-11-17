package edu.lab.service;

import edu.lab.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 用户注册与身份校验业务。 */
@Service
public class AuthService {
    private static final Set<String> REGISTER_ROLES = Set.of("STUDENT", "TEACHER");
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
        String storedPassword = String.valueOf(user.get("password"));
        boolean encoded = storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$");
        boolean matches = encoded
                ? passwordEncoder.matches(password, storedPassword)
                : storedPassword.equals(password);
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (!encoded) {
            userRepository.updatePassword(((Number) user.get("id")).longValue(), passwordEncoder.encode(password));
        }
        user.remove("password");
        return user;
    }

    public void register(String username, String password, String realName, String requestedRole) {
        String role = requestedRole == null ? "STUDENT" : requestedRole.toUpperCase(Locale.ROOT);
        if (!REGISTER_ROLES.contains(role)) {
            role = "STUDENT";
        }
        String normalizedUsername = username.trim();
        if (!normalizedUsername.matches("[A-Za-z0-9_.-]{3,50}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号只能包含字母、数字、点、下划线和短横线");
        }
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少需要 8 个字符");
        }
        if (userRepository.exists(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该账号已存在");
        }
        userRepository.save(
                normalizedUsername, passwordEncoder.encode(password), realName.trim(), role);
    }
}
