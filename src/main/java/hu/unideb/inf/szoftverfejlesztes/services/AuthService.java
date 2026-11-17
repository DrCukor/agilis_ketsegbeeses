package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    // felhasznalonev@domain.tld formátum, pl. kisspista12@gmail.com
    // Ékezetes betűk is megengedettek (pl. Ádám@gmail.com)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}-]+(\\.[\\p{L}\\p{N}-]+)*\\.\\p{L}{2,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String email, String name, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Érvénytelen email cím!");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A név nem lehet üres!");
        }
        // Bármilyen jelszó elfogadott, csak üres nem lehet
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("A jelszó nem lehet üres!");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Ezzel az email címmel már regisztráltak!");
        }
        // A kiadások név alapján hivatkoznak a felhasználókra, ezért a név is egyedi
        String trimmedName = name.trim();
        if (userRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Ez a név már foglalt!");
        }

        User user = new User(normalizedEmail, trimmedName, passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    // Sikeres belépésnél a felhasználót adja vissza, hibás adatoknál üreset
    public Optional<User> login(String email, String password) {
        if (password == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()));
    }

    public Optional<User> findById(Long id) {
        return id == null ? Optional.empty() : userRepository.findById(id);
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
