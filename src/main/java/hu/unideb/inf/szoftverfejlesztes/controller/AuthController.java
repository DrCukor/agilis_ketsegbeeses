package hu.unideb.inf.szoftverfejlesztes.controller;

import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // A bejelentkezett felhasználó azonosítója ezen a kulcson van a sessionben
    public static final String SESSION_USER_ID = "userId";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            User user = authService.register(request.getEmail(), request.getName(), request.getPassword());
            // Sikeres regisztráció után automatikusan be is léptetjük
            startSession(httpRequest, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(user -> {
                    startSession(httpRequest, user);
                    return ResponseEntity.ok(UserResponse.from(user));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Hibás email cím vagy jelszó!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        Long userId = session == null ? null : (Long) session.getAttribute(SESSION_USER_ID);
        return authService.findById(userId)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nincs bejelentkezve!"));
    }

    private void startSession(HttpServletRequest httpRequest, User user) {
        HttpSession session = httpRequest.getSession(true);
        // Új session azonosító belépéskor (session fixation elleni védelem)
        httpRequest.changeSessionId();
        session.setAttribute(SESSION_USER_ID, user.getId());
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

    // A jelszó hash-t soha nem küldjük ki a kliensnek
    public record UserResponse(Long id, String email, String name) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getName());
        }
    }
}
