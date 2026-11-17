package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.repository.DebtRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class BudgetService {

    public static final List<String> ALL_USERS = List.of("Ádám", "Dávid", "Levente", "Zoltán");

    private final ExpenseRepository expenseRepository;
    private final DebtRepository debtRepository;
    private final DebtService debtService;

    public BudgetService(ExpenseRepository expenseRepository, DebtRepository debtRepository, DebtService debtService) {
        this.expenseRepository = expenseRepository;
        this.debtRepository = debtRepository;
        this.debtService = debtService;
    }

    public List<String> getAllUsers() {
        return ALL_USERS;
    }

    @Transactional
    public Expense createExpense(String title, double amount, String payer, List<String> participants) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A megnevezés nem lehet üres!");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Az összegnek pozitívnak kell lennie!");
        }
        if (payer == null || !ALL_USERS.contains(payer)) {
            throw new IllegalArgumentException("Ismeretlen fizető: " + payer);
        }
        if (participants == null || participants.isEmpty()) {
            participants = new ArrayList<>(ALL_USERS);
        } else {
            // Ismétlődő nevek kiszűrése, a sorrend megtartásával
            participants = new ArrayList<>(new LinkedHashSet<>(participants));
            for (String participant : participants) {
                if (!ALL_USERS.contains(participant)) {
                    throw new IllegalArgumentException("Ismeretlen résztvevő: " + participant);
                }
            }
            // Ha csak maga a fizető a résztvevő, nem keletkezne tartozás
            if (participants.size() == 1 && participants.get(0).equals(payer)) {
                throw new IllegalArgumentException("Legalább egy résztvevőnek a fizetőn kívül kell lennie!");
            }
        }

        Expense expense = new Expense(title, amount, payer, participants);
        Expense savedExpense = expenseRepository.save(expense);

        List<Debt> rawDebts = debtService.calculateDebts(savedExpense);
        debtRepository.saveAll(rawDebts);

        return savedExpense;
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAllByOrderByIdDesc();
    }

    public List<Debt> getSimplifiedBalances() {
        List<Debt> rawDebts = debtRepository.findAll();
        return debtService.simplifyDebts(rawDebts);
    }
}
