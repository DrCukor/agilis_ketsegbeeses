package hu.unideb.inf.szoftverfejlesztes.repository;

import hu.unideb.inf.szoftverfejlesztes.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    // Egy csoport ismétlődő kiadásai, a következő dátum szerint
    List<RecurringExpense> findAllByGroupIdOrderByNextRunDateAsc(Long groupId);

    // Amelyeket ma (vagy korábban) kellett volna létrehozni
    List<RecurringExpense> findAllByActiveTrueAndNextRunDateLessThanEqual(LocalDate date);
}
