package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.DebtRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class BudgetService {

    private final ExpenseRepository expenseRepository;
    private final DebtRepository debtRepository;
    private final UserRepository userRepository;
    private final DebtService debtService;

    public BudgetService(ExpenseRepository expenseRepository, DebtRepository debtRepository,
                         UserRepository userRepository, DebtService debtService) {
        this.expenseRepository = expenseRepository;
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
        this.debtService = debtService;
    }

    // A kiadásokban szereplő személyek: minden regisztrált felhasználó az admin kivételével,
    // magyar ábécérendben (Ádám, Dávid, Levi, Zoli)
    public List<String> getAllUsers() {
        Collator hungarian = Collator.getInstance(Locale.forLanguageTag("hu-HU"));
        return userRepository.findAllByAdminFalse().stream()
                .map(User::getName)
                .sorted(hungarian)
                .toList();
    }

    @Transactional
    public Expense createExpense(String title, double amount, String payer, List<String> participants) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A megnevezés nem lehet üres!");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Az összegnek pozitívnak kell lennie!");
        }

        List<String> allUsers = getAllUsers();
        if (payer == null || !allUsers.contains(payer)) {
            throw new IllegalArgumentException("Ismeretlen fizető: " + payer);
        }
        if (participants == null || participants.isEmpty()) {
            participants = new ArrayList<>(allUsers);
        } else {
            // Ismétlődő nevek kiszűrése, a sorrend megtartásával
            participants = new ArrayList<>(new LinkedHashSet<>(participants));
            for (String participant : participants) {
                if (!allUsers.contains(participant)) {
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
