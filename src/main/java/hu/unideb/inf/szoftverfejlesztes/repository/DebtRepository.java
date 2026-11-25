package hu.unideb.inf.szoftverfejlesztes.repository;

import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    // Egy kiadáshoz tartozó nyers tartozások törlése (kiadás törlésekor / szerkesztésekor)
    @Modifying
    @Query("delete from Debt d where d.expense.id = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);

    // Egy csoport kiadásaiból származó nyers tartozások
    @Query("select d from Debt d where d.expense.group.id = :groupId")
    List<Debt> findAllByGroupId(@Param("groupId") Long groupId);

    // Egy csoport összes nyers tartozásának törlése
    @Modifying
    @Query("delete from Debt d where d.expense.id in (select e.id from Expense e where e.group.id = :groupId)")
    void deleteAllByGroupId(@Param("groupId") Long groupId);
}
