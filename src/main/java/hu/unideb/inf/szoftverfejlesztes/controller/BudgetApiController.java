package hu.unideb.inf.szoftverfejlesztes.controller;

import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.services.BudgetService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Data
    public static class ExpenseRequest {
        private String title;
        private double amount;
        private String payer;
        private List<String> participants;
    }
}
