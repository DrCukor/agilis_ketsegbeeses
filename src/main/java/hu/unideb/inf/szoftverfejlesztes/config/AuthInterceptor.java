package hu.unideb.inf.szoftverfejlesztes.config;

import hu.unideb.inf.szoftverfejlesztes.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

// Bejelentkezés nélkül (vagy lejárt munkamenettel) a védett API végpontok 401-et adnak vissza;
// sikeres ellenőrzésnél a bejelentkezett felhasználó request attribútumba kerül
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Optional<User> currentUser = sessionManager.getCurrentUser(request);
        if (currentUser.isPresent()) {
            request.setAttribute(SessionManager.CURRENT_USER_ATTRIBUTE, currentUser.get());
            return true;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("Bejelentkezés szükséges!");
        return false;
    }
}
