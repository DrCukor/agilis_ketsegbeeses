package hu.unideb.inf.szoftverfejlesztes.controller;

import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import hu.unideb.inf.szoftverfejlesztes.services.BudgetService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

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
    public ResponseEntity<?> addExpense(@RequestBody ExpenseRequest request) {
        try {
            Expense saved = budgetService.createExpense(
                    request.getTitle(),
                    request.getAmount(),
                    request.getPayer(),
                    request.getParticipants()
            );
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody ExpenseRequest request) {
        try {
            Expense saved = budgetService.updateExpense(
                    id,
                    request.getTitle(),
                    request.getAmount(),
                    request.getPayer(),
                    request.getParticipants()
            );
            return ResponseEntity.ok(saved);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id) {
        try {
            budgetService.deleteExpense(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/settlements")
    public ResponseEntity<?> settleDebt(@RequestBody SettlementRequest request) {
        try {
            Settlement saved = budgetService.settleDebt(request.getDebtor(), request.getCreditor());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class ExpenseRequest {
        private String title;
        private double amount;
        private String payer;
        private List<String> participants;
    }

    @Data
    public static class SettlementRequest {
        private String debtor;
        private String creditor;
    }
}
