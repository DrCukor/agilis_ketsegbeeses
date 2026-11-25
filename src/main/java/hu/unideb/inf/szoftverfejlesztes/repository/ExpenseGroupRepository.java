package hu.unideb.inf.szoftverfejlesztes.repository;

import hu.unideb.inf.szoftverfejlesztes.model.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {
    Optional<ExpenseGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    List<ExpenseGroup> findAllByOrderByNameAsc();
}
