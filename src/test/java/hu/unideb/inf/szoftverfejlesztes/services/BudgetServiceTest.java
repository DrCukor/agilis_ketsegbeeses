package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DebtService debtService;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void deleteAllData_shouldRejectNonAdmin() {
        User user = new User(
                "adam@gmail.com",
                "Ádám",
                "hashedPassword",
                false
        );

        assertThrows(
                ForbiddenException.class,
                () -> budgetService.deleteAllData(user)
        );

        verifyNoInteractions(
                expenseRepository,
                debtRepository,
                settlementRepository
        );
    }

    @Test
    void deleteAllData_shouldAllowAdmin() {
        User admin = new User(
                "admin@gmail.com",
                "Admin",
                "hashedPassword",
                true
        );

        budgetService.deleteAllData(admin);

        verify(settlementRepository).deleteAllInBatch();
        verify(debtRepository).deleteAllInBatch();
        verify(expenseRepository).deleteAll();
    }
}
