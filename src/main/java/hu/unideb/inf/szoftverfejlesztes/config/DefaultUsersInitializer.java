package hu.unideb.inf.szoftverfejlesztes.config;

import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.UserRepository;
import hu.unideb.inf.szoftverfejlesztes.services.AuthService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

// Indításkor létrehozza az alapértelmezett felhasználókat, ha még nincsenek az adatbázisban
@Component
public class DefaultUsersInitializer implements ApplicationRunner {

    private record DefaultUser(String email, String name, String password, boolean admin) {
    }

    private static final List<DefaultUser> DEFAULT_USERS = List.of(
            // Tesztelési célú admin: be tud lépni, de nem szerepel a kiadásokban
            new DefaultUser("admin@gmail.com", "admin", "admin", true),
            // A csapat tagjai
            new DefaultUser("Zoli@gmail.com", "Zoli", "Zoli", false),
            new DefaultUser("Levi@gmail.com", "Levi", "Levi", false),
            new DefaultUser("Ádám@gmail.com", "Ádám", "Ádám", false),
            new DefaultUser("Dávid@gmail.com", "Dávid", "Dávid", false)
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUsersInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (DefaultUser defaultUser : DEFAULT_USERS) {
            String email = AuthService.normalizeEmail(defaultUser.email());
            if (!userRepository.existsByEmail(email)) {
                userRepository.save(new User(email, defaultUser.name(),
                        passwordEncoder.encode(defaultUser.password()), defaultUser.admin()));
            }
        }
    }
}
