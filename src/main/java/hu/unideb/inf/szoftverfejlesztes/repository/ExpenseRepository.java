package hu.unideb.inf.szoftverfejlesztes.repository;

import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByOrderByIdDesc();

    // Egy csoport kiadásai, a legújabb elöl
    List<Expense> findAllByGroupIdOrderByIdDesc(Long groupId);

    // Csoport nélküli (a csoportok bevezetése előtti) kiadások
    List<Expense> findAllByGroupIsNull();
}
