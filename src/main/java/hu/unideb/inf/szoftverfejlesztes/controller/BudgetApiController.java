package hu.unideb.inf.szoftverfejlesztes.controller;

import hu.unideb.inf.szoftverfejlesztes.config.SessionManager;
import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.RecurringExpense;
import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.services.BudgetService;
import hu.unideb.inf.szoftverfejlesztes.services.ExpenseInput;
import hu.unideb.inf.szoftverfejlesztes.services.RecurringExpenseService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Egy csoport kiadásai, egyenlegei, rendezései és ismétlődő kiadásai.
// A hibák (400 / 403 / 404) kezelése a GlobalExceptionHandler-ben történik.
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BudgetApiController {

    private final BudgetService budgetService;
    private final RecurringExpenseService recurringExpenseService;

    public BudgetApiController(BudgetService budgetService, RecurringExpenseService recurringExpenseService) {
        this.budgetService = budgetService;
        this.recurringExpenseService = recurringExpenseService;
    }

    // A csoport tagjai (ők lehetnek fizetők és résztvevők)
    @GetMapping("/groups/{groupId}/members")
    public List<String> getMembers(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                   @PathVariable Long groupId) {
        return budgetService.getMembers(currentUser, groupId);
    }

    @GetMapping("/groups/{groupId}/expenses")
    public List<Expense> getExpenses(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                     @PathVariable Long groupId) {
        return budgetService.getExpenses(currentUser, groupId);
    }

    @GetMapping("/groups/{groupId}/balances")
    public List<Debt> getBalances(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                  @PathVariable Long groupId) {
        return budgetService.getBalances(currentUser, groupId);
    }

    @PostMapping("/groups/{groupId}/expenses")
    public Expense addExpense(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                              @PathVariable Long groupId, @RequestBody ExpenseRequest request) {
        return budgetService.createExpense(currentUser, groupId, request.toInput());
    }

    @PutMapping("/groups/{groupId}/expenses/{id}")
    public Expense updateExpense(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                 @PathVariable Long groupId, @PathVariable Long id, @RequestBody ExpenseRequest request) {
        return budgetService.updateExpense(currentUser, groupId, id, request.toInput());
    }

    @DeleteMapping("/groups/{groupId}/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                              @PathVariable Long groupId, @PathVariable Long id) {
        budgetService.deleteExpense(currentUser, groupId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups/{groupId}/settlements")
    public List<Settlement> getSettlements(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                           @PathVariable Long groupId) {
        return budgetService.getSettlements(currentUser, groupId);
    }

    @PostMapping("/groups/{groupId}/settlements")
    public List<Settlement> settleDebt(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                       @PathVariable Long groupId, @RequestBody SettlementRequest request) {
        return budgetService.settleDebt(currentUser, groupId, request.getDebtor(), request.getCreditor(), request.getCurrency());
    }

    // Tulajdonos / admin: a csoport összes kiadásának, tartozásának, rendezésének és ismétlődő kiadásának törlése
    @DeleteMapping("/groups/{groupId}/data")
    public ResponseEntity<Void> deleteGroupData(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                                @PathVariable Long groupId) {
        budgetService.deleteGroupData(currentUser, groupId);
        recurringExpenseService.deleteAllForGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups/{groupId}/recurring")
    public List<RecurringExpense> getRecurring(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                               @PathVariable Long groupId) {
        return recurringExpenseService.getRecurringExpenses(currentUser, groupId);
    }

    // Új ismétlődő kiadás (az első kiadás azonnal létrejön)
    @PostMapping("/groups/{groupId}/recurring")
    public RecurringExpense addRecurring(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                         @PathVariable Long groupId, @RequestBody ExpenseRequest request) {
        return recurringExpenseService.create(currentUser, groupId, request.toInput(), request.getDayOfMonth());
    }

    @DeleteMapping("/groups/{groupId}/recurring/{id}")
    public ResponseEntity<Void> deleteRecurring(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser,
                                                @PathVariable Long groupId, @PathVariable Long id) {
        recurringExpenseService.delete(currentUser, groupId, id);
        return ResponseEntity.noContent().build();
    }

    // Rendszer-admin: minden csoport összes adatának törlése
    @DeleteMapping("/admin/data")
    public ResponseEntity<Void> deleteAllData(@RequestAttribute(SessionManager.CURRENT_USER_ATTRIBUTE) User currentUser) {
        budgetService.deleteAllData(currentUser);
        recurringExpenseService.deleteAll();
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
        private String splitType;
        private Map<String, Double> splitValues;
        // Csak ismétlődő kiadásnál: a hónap hányadik napján jöjjön létre (1–28)
        private Integer dayOfMonth;

        ExpenseInput toInput() {
            return new ExpenseInput(title, amount, currency, category, payer, participants, splitType, splitValues);
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
