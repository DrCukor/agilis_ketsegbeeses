package hu.unideb.inf.szoftverfejlesztes.repository;

import hu.unideb.inf.szoftverfejlesztes.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNameIgnoreCase(String name);

    // A kiadásokban csak a nem-admin felhasználók szerepelhetnek
    List<User> findAllByAdminFalse();
}
