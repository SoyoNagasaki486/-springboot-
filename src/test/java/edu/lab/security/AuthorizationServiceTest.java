package edu.lab.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceTest {
    private final AuthorizationService service = new AuthorizationService(new CurrentUserService());

    @Test
    void shouldKeepStudentOutOfAdministrationResources() {
        MockHttpSession session = sessionFor("STUDENT");

        assertDoesNotThrow(() -> service.requireRead("labs", session));
        assertThrows(ResponseStatusException.class, () -> service.requireRead("users", session));
        assertThrows(ResponseStatusException.class, () -> service.requireWrite("equipment", session));
    }

    @Test
    void shouldAllowTeacherToMaintainSafetyResourcesButNotUsers() {
        MockHttpSession session = sessionFor("TEACHER");

        assertDoesNotThrow(() -> service.requireWrite("checks", session));
        assertThrows(ResponseStatusException.class, () -> service.requireWrite("users", session));
    }

    private MockHttpSession sessionFor(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                CurrentUserService.SESSION_KEY,
                new SessionUser(1L, role.toLowerCase(), "测试用户", role, false));
        return session;
    }
}
