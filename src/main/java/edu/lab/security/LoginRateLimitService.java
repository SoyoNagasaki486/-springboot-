package edu.lab.security;

import edu.lab.config.LabGuardProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** 单实例内存登录失败限制，防止短时间暴力尝试。 */
@Service
public class LoginRateLimitService {
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final LabGuardProperties properties;

    public LoginRateLimitService(LabGuardProperties properties) {
        this.properties = properties;
    }

    public void checkAllowed(String username, String remoteAddress) {
        String key = key(username, remoteAddress);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        if (attempt.blockedUntil() != null && attempt.blockedUntil().isAfter(Instant.now())) {
            long minutes = Math.max(1, Duration.between(Instant.now(), attempt.blockedUntil()).toMinutes() + 1);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁，请在 " + minutes + " 分钟后重试");
        }
        if (attempt.blockedUntil() != null) {
            attempts.remove(key);
        }
    }

    public void failed(String username, String remoteAddress) {
        String key = key(username, remoteAddress);
        attempts.compute(key, (ignored, current) -> {
            int failures = current == null ? 1 : current.failures() + 1;
            Instant blockedUntil = failures >= properties.getSecurity().getMaxLoginAttempts()
                    ? Instant.now().plus(Duration.ofMinutes(properties.getSecurity().getLoginBlockMinutes()))
                    : null;
            return new Attempt(failures, blockedUntil);
        });
    }

    public void succeeded(String username, String remoteAddress) {
        attempts.remove(key(username, remoteAddress));
    }

    private String key(String username, String remoteAddress) {
        return username.trim().toLowerCase(java.util.Locale.ROOT) + "|" + remoteAddress;
    }

    private record Attempt(int failures, Instant blockedUntil) {}
}
