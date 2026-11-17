package hu.unideb.inf.szoftverfejlesztes.repository;

import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    // Egy kiadáshoz tartozó nyers tartozások törlése (kiadás törlésekor / szerkesztésekor)
    @Modifying
    @Query("delete from Debt d where d.expense.id = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);
}
