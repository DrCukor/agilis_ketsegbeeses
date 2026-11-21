package hu.unideb.inf.szoftverfejlesztes.config;

import hu.unideb.inf.szoftverfejlesztes.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AuthInterceptor authInterceptor;

    @BeforeEach
    void setUp() {
        authInterceptor = new AuthInterceptor(sessionManager);
    }

    @Test
    void preHandle_shouldAllowAuthenticatedUser() throws Exception {
        User user = new User(
                "adam@gmail.com",
                "Ádám",
                "hashedPassword"
        );

        when(sessionManager.getCurrentUser(request))
                .thenReturn(Optional.of(user));

        boolean result =
                authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);

        verify(request).setAttribute(
                SessionManager.CURRENT_USER_ATTRIBUTE,
                user
        );

        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void preHandle_shouldRejectUnauthenticatedUser() throws Exception {
        when(sessionManager.getCurrentUser(request))
                .thenReturn(Optional.empty());

        when(response.getWriter())
                .thenReturn(new PrintWriter(new StringWriter()));

        boolean result =
                authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);

        verify(response).setStatus(401);
        verify(response).setContentType("text/plain");
        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    void preHandle_shouldReturnCorrectErrorMessage() throws Exception {
        when(sessionManager.getCurrentUser(request))
                .thenReturn(Optional.empty());

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(printWriter);

        authInterceptor.preHandle(request, response, new Object());

        printWriter.flush();

        assertEquals(
                "Bejelentkezés szükséges!",
                stringWriter.toString()
        );
    }

    @Test
    void preHandle_shouldNotSetUserAttributeWhenUnauthenticated()
            throws Exception {

        when(sessionManager.getCurrentUser(request))
                .thenReturn(Optional.empty());

        when(response.getWriter())
                .thenReturn(new PrintWriter(new StringWriter()));

        boolean result =
                authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);

        verify(request, never()).setAttribute(
                eq(SessionManager.CURRENT_USER_ATTRIBUTE),
                any()
        );
    }

    @Test
    void preHandle_shouldNotModifyResponseWhenAuthenticated()
            throws Exception {

        User user = new User(
                "levi@gmail.com",
                "Levi",
                "hashedPassword"
        );

        when(sessionManager.getCurrentUser(request))
                .thenReturn(Optional.of(user));

        boolean result =
                authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);

        verifyNoInteractions(response);
    }
}
