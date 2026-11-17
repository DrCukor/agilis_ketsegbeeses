package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.DebtRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.SettlementRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class BudgetService {

    private final ExpenseRepository expenseRepository;
    private final DebtRepository debtRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final DebtService debtService;

    public BudgetService(ExpenseRepository expenseRepository, DebtRepository debtRepository,
                         SettlementRepository settlementRepository, UserRepository userRepository,
                         DebtService debtService) {
        this.expenseRepository = expenseRepository;
        this.debtRepository = debtRepository;
        this.settlementRepository = settlementRepository;
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
        List<String> validParticipants = validateExpense(title, amount, payer, participants);

        Expense expense = new Expense(title.trim(), amount, payer, validParticipants);
        Expense savedExpense = expenseRepository.save(expense);

        debtRepository.saveAll(debtService.calculateDebts(savedExpense));
        return savedExpense;
    }

    @Transactional
    public Expense updateExpense(Long id, String title, double amount, String payer, List<String> participants) {
        Expense expense = findExpense(id);
        List<String> validParticipants = validateExpense(title, amount, payer, participants);

        expense.setTitle(title.trim());
        expense.setAmount(amount);
        expense.setPayer(payer);
        expense.setParticipants(new ArrayList<>(validParticipants));
        Expense savedExpense = expenseRepository.save(expense);

        // A régi nyers tartozások helyett újakat számolunk a módosított adatokból
        debtRepository.deleteByExpenseId(id);
        debtRepository.saveAll(debtService.calculateDebts(savedExpense));
        return savedExpense;
    }

    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = findExpense(id);
        debtRepository.deleteByExpenseId(id);
        expenseRepository.delete(expense);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAllByOrderByIdDesc();
    }

    public List<Debt> getSimplifiedBalances() {
        List<Debt> allDebts = new ArrayList<>(debtRepository.findAll());
        // A kiegyenlítés fordított irányú tartozásként nettósítja az eredetit
        for (Settlement settlement : settlementRepository.findAll()) {
            allDebts.add(new Debt(settlement.getCreditor(), settlement.getDebtor(), settlement.getAmount()));
        }
        return debtService.simplifyDebts(allDebts);
    }

    // Az adós -> hitelező egyszerűsített tartozás teljes kiegyenlítése
    @Transactional
    public Settlement settleDebt(String debtor, String creditor) {
        Debt balance = getSimplifiedBalances().stream()
                .filter(d -> d.getDebtor().equals(debtor) && d.getCreditor().equals(creditor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nincs rendezendő tartozás: " + debtor + " -> " + creditor));
        return settlementRepository.save(new Settlement(debtor, creditor, balance.getAmount()));
    }

    private Expense findExpense(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Nem található kiadás: " + id));
    }

    // Közös ellenőrzés létrehozáshoz és szerkesztéshez; a végleges résztvevő-listát adja vissza
    private List<String> validateExpense(String title, double amount, String payer, List<String> participants) {
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
            return new ArrayList<>(allUsers);
        }

        // Ismétlődő nevek kiszűrése, a sorrend megtartásával
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(participants));
        for (String participant : unique) {
            if (!allUsers.contains(participant)) {
                throw new IllegalArgumentException("Ismeretlen résztvevő: " + participant);
            }
        }
        // Ha csak maga a fizető a résztvevő, nem keletkezne tartozás
        if (unique.size() == 1 && unique.get(0).equals(payer)) {
            throw new IllegalArgumentException("Legalább egy résztvevőnek a fizetőn kívül kell lennie!");
        }
        return unique;
    }
}
