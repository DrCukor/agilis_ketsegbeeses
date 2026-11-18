package hu.unideb.inf.szoftverfejlesztes.controller;

import hu.unideb.inf.szoftverfejlesztes.config.SessionManager;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionManager sessionManager;

    public AuthController(AuthService authService, SessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        User user = authService.register(request.getEmail(), request.getName(), request.getPassword());
        // Sikeres regisztráció után automatikusan be is léptetjük
        sessionManager.startSession(httpRequest, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user, httpRequest, sessionManager));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(user -> {
                    sessionManager.startSession(httpRequest, user);
                    return ResponseEntity.ok(UserResponse.from(user, httpRequest, sessionManager));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Hibás email cím vagy jelszó!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        sessionManager.endSession(httpRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest httpRequest) {
        return sessionManager.getCurrentUser(httpRequest)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResponse.from(user, httpRequest, sessionManager)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nincs bejelentkezve!"));
    }

    @Data
    public static class RegisterRequest {
        private String email;
        private String name;
        private String password;
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    // A jelszó hash-t soha nem küldjük ki a kliensnek; a hátralévő idő a frontend kijelentkeztető időzítőjéhez kell
    public record UserResponse(Long id, String email, String name, boolean admin, long sessionRemainingSeconds) {
        static UserResponse from(User user, HttpServletRequest request, SessionManager sessionManager) {
            return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.isAdmin(),
                    sessionManager.getRemainingSeconds(request));
        }
    }
}
