package edu.lab.security;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {
    @Mock
    private HttpSession session;
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService();
    }

    @Test
    void shouldReturnLoggedInUser() {
        SessionUser user = new SessionUser(1L, "student", "张同学", "STUDENT", true);
        when(session.getAttribute(CurrentUserService.SESSION_KEY)).thenReturn(user);

        assertEquals(user, currentUserService.require(session));
    }

    @Test
    void shouldRejectMissingSessionIdentity() {
        assertThrows(ResponseStatusException.class, () -> currentUserService.require(session));
    }

    @Test
    void shouldRejectRoleOutsideAllowedSet() {
        SessionUser user = new SessionUser(1L, "student", "张同学", "STUDENT", true);
        when(session.getAttribute(CurrentUserService.SESSION_KEY)).thenReturn(user);

        assertThrows(ResponseStatusException.class,
                () -> currentUserService.requireRole(session, "ADMIN", "TEACHER"));
    }
}
