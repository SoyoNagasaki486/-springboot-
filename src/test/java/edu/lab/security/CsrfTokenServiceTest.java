package edu.lab.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsrfTokenServiceTest {
    private final CsrfTokenService service = new CsrfTokenService();

    @Test
    void shouldReuseTokenWithinSameSessionAndValidateHeader() {
        MockHttpSession session = new MockHttpSession();
        String token = service.token(session);
        assertEquals(token, service.token(session));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.addHeader(CsrfTokenService.HEADER_NAME, token);
        service.validate(request);
    }

    @Test
    void shouldRejectMissingOrMismatchedToken() {
        MockHttpSession session = new MockHttpSession();
        service.token(session);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.addHeader(CsrfTokenService.HEADER_NAME, "invalid-token");

        assertThrows(ResponseStatusException.class, () -> service.validate(request));
    }
}
