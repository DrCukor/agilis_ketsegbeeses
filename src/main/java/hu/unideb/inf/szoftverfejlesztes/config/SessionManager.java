package hu.unideb.inf.szoftverfejlesztes.config;

import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

// Bejelentkezési munkamenet kezelése: belépés, kilépés, aktuális felhasználó, lejárat
@Component
public class SessionManager {

    // A védett végpontoknál ezen a request attribútumon érhető el a bejelentkezett User
    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private static final String SESSION_USER_ID = "userId";
    private static final String SESSION_LOGIN_TIME = "loginTime";

    private final AuthService authService;
    // A munkamenet a belépéstől számítva ennyi ideig él (aktivitástól függetlenül)
    private final Duration maxDuration;

    public SessionManager(AuthService authService,
                          @Value("${kozoskassza.session.max-duration:15m}") Duration maxDuration) {
        this.authService = authService;
        this.maxDuration = maxDuration;
    }

    public void startSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        // Új session azonosító belépéskor (session fixation elleni védelem)
        request.changeSessionId();
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_LOGIN_TIME, Instant.now());
    }

    public void endSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    // A bejelentkezett felhasználó; lejárt munkamenetnél kilépteti és üreset ad vissza
    public Optional<User> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        if (getRemainingSeconds(session) <= 0) {
            session.invalidate();
            return Optional.empty();
        }
        return authService.findById((Long) session.getAttribute(SESSION_USER_ID));
    }

    // Hány másodperc van hátra a munkamenetből (a frontend időzítőjéhez)
    public long getRemainingSeconds(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? 0 : Math.max(0, getRemainingSeconds(session));
    }

    private long getRemainingSeconds(HttpSession session) {
        Instant loginTime = (Instant) session.getAttribute(SESSION_LOGIN_TIME);
        if (loginTime == null) {
            return 0;
        }
        return Duration.between(Instant.now(), loginTime.plus(maxDuration)).getSeconds();
    }
}
