package edu.lab.security;

import edu.lab.config.LabGuardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimitServiceTest {
    @Test
    void shouldBlockAfterConfiguredFailuresAndClearOnSuccess() {
        LabGuardProperties properties = new LabGuardProperties();
        properties.getSecurity().setMaxLoginAttempts(2);
        properties.getSecurity().setLoginBlockMinutes(10);
        LoginRateLimitService service = new LoginRateLimitService(properties);

        service.failed("student", "127.0.0.1");
        assertDoesNotThrow(() -> service.checkAllowed("student", "127.0.0.1"));
        service.failed("student", "127.0.0.1");
        assertThrows(ResponseStatusException.class, () -> service.checkAllowed("student", "127.0.0.1"));

        service.succeeded("student", "127.0.0.1");
        assertDoesNotThrow(() -> service.checkAllowed("student", "127.0.0.1"));
    }
}
