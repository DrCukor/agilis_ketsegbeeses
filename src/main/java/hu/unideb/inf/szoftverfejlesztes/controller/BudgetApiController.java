package hu.unideb.inf.szoftverfejlesztes.controller;

import hu.unideb.inf.szoftverfejlesztes.config.SessionManager;
import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.services.BudgetService;
import hu.unideb.inf.szoftverfejlesztes.services.ExpenseInput;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// A hibák (400 / 403 / 404) kezelése a GlobalExceptionHandler-ben történik
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BudgetApiController {

    private final BudgetService budgetService;

    public BudgetApiController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/users")
    public List<String> getUsers() {
        return budgetService.getAllUsers();
    }

    @GetMapping("/expenses")
    public List<Expense> getExpenses() {
        return budgetService.getAllExpenses();
    }

    @GetMapping("/balances")
    public List<Debt> getBalances() {
        return budgetService.getSimplifiedBalances();
    }

    @PostMapping("/expenses")
    public Expense addExpense(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                              @RequestBody ExpenseRequest request) {
        return budgetService.createExpense(currentUser, request.toInput());
    }

    @PutMapping("/expenses/{id}")
    public Expense updateExpense(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                 @PathVariable Long id, @RequestBody ExpenseRequest request) {
        return budgetService.updateExpense(currentUser, id, request.toInput());
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                              @PathVariable Long id) {
        budgetService.deleteExpense(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/settlements")
    public List<Settlement> settleDebt(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                       @RequestBody SettlementRequest request) {
        return budgetService.settleDebt(currentUser, request.getDebtor(), request.getCreditor(), request.getCurrency());
    }

    // Admin: minden kiadás, tartozás és rendezés törlése
    @DeleteMapping("/admin/data")
    public ResponseEntity<Void> deleteAllData(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser) {
        budgetService.deleteAllData(currentUser);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class ExpenseRequest {
        private String title;
        private Double amount;
        private String currency;
        private String category;
        private String payer;
        private List<String> participants;

        ExpenseInput toInput() {
            return new ExpenseInput(title, amount, currency, category, payer, participants);
        }
    }

    @Data
    public static class SettlementRequest {
        private String debtor;
        private String creditor;
        // Melyik pénznemben rendez (HUF, EUR, USD, GBP); üresen hagyva mindegyikben
        private String currency;
    }
}
