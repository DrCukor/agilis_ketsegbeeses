package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mindig kisbetűsítve tároljuk, így az egyediség kis-nagybetű független
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    // Soha nem a nyers jelszót tároljuk, csak a BCrypt hash-t
    @Column(nullable = false)
    private String passwordHash;

    // Az admin be tud lépni, de nem szerepel fizetőként / résztvevőként a kiadásokban
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean admin;

    public User(String email, String name, String passwordHash) {
        this(email, name, passwordHash, false);
    }

    public User(String email, String name, String passwordHash, boolean admin) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.admin = admin;
    }
}
