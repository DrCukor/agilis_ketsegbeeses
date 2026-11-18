package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Category;
import hu.unideb.inf.szoftverfejlesztes.model.Currency;
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
    public Expense createExpense(User currentUser, ExpenseInput input) {
        ValidExpense valid = validateExpense(currentUser, input);

        Expense expense = new Expense(valid.title(), valid.amount(), valid.currency(), valid.category(),
                valid.payer(), valid.participants(), currentUser.getName());
        Expense savedExpense = expenseRepository.save(expense);

        debtRepository.saveAll(debtService.calculateDebts(savedExpense));
        return savedExpense;
    }

    @Transactional
    public Expense updateExpense(User currentUser, Long id, ExpenseInput input) {
        Expense expense = findExpense(id);
        checkCanModify(currentUser, expense);
        ValidExpense valid = validateExpense(currentUser, input);

        expense.setTitle(valid.title());
        expense.setAmount(valid.amount());
        expense.setCurrency(valid.currency());
        expense.setCategory(valid.category());
        expense.setPayer(valid.payer());
        expense.setParticipants(new ArrayList<>(valid.participants()));
        Expense savedExpense = expenseRepository.save(expense);

        // A régi nyers tartozások helyett újakat számolunk a módosított adatokból
        debtRepository.deleteByExpenseId(id);
        debtRepository.saveAll(debtService.calculateDebts(savedExpense));
        return savedExpense;
    }

    @Transactional
    public void deleteExpense(User currentUser, Long id) {
        Expense expense = findExpense(id);
        checkCanModify(currentUser, expense);
        debtRepository.deleteByExpenseId(id);
        expenseRepository.delete(expense);
    }

    // Admin: minden kiadás, tartozás és rendezés törlése (a felhasználók megmaradnak)
    @Transactional
    public void deleteAllData(User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Az összes adatot csak az admin törölheti!");
        }
        settlementRepository.deleteAllInBatch();
        debtRepository.deleteAllInBatch();
        // Egyenként töröljük, hogy a résztvevő-lista (expense_participants) is törlődjön
        expenseRepository.deleteAll();
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAllByOrderByIdDesc();
    }

    public List<Debt> getSimplifiedBalances() {
        List<Debt> allDebts = new ArrayList<>(debtRepository.findAll());
        // A kiegyenlítés fordított irányú tartozásként nettósítja az eredetit
        for (Settlement settlement : settlementRepository.findAll()) {
            allDebts.add(new Debt(settlement.getCreditor(), settlement.getDebtor(),
                    settlement.getAmount(), settlement.getCurrency()));
        }
        return debtService.simplifyDebts(allDebts);
    }

    // Az adós -> hitelező tartozás teljes kiegyenlítése a megadott pénznemben
    // (ha nincs megadva pénznem, akkor mindegyikben).
    // Csak a hitelező jelölheti (ő tudja, hogy megkapta a pénzt), illetve az admin.
    @Transactional
    public List<Settlement> settleDebt(User currentUser, String debtor, String creditor, String currencyCode) {
        if (!currentUser.isAdmin() && !currentUser.getName().equals(creditor)) {
            throw new ForbiddenException("Csak a hitelező jelölheti a tartozást rendezettnek!");
        }
        Currency currency = currencyCode == null || currencyCode.isBlank() ? null : Currency.parse(currencyCode);

        List<Debt> balances = getSimplifiedBalances().stream()
                .filter(d -> d.getDebtor().equals(debtor) && d.getCreditor().equals(creditor))
                .filter(d -> currency == null || d.getCurrency() == currency)
                .toList();
        if (balances.isEmpty()) {
            String currencyText = currency == null ? "" : " " + currency.name();
            throw new IllegalArgumentException("Nincs rendezendő" + currencyText + " tartozás: " + debtor + " -> " + creditor);
        }

        return balances.stream()
                .map(b -> settlementRepository.save(new Settlement(debtor, creditor, b.getAmount(), b.getCurrency())))
                .toList();
    }

    private Expense findExpense(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Nem található kiadás: " + id));
    }

    // Csak a saját rögzítésű kiadás módosítható / törölhető; az admin bármelyiket.
    // A createdBy nélküli (régi) tételeket csak az admin kezelheti.
    private void checkCanModify(User currentUser, Expense expense) {
        if (currentUser.isAdmin()) {
            return;
        }
        if (expense.getCreatedBy() == null || !expense.getCreatedBy().equals(currentUser.getName())) {
            throw new ForbiddenException("Csak a saját kiadásaidat módosíthatod vagy törölheted!");
        }
    }

    private record ValidExpense(String title, double amount, Currency currency, Category category,
                                String payer, List<String> participants) {
    }

    // Közös ellenőrzés létrehozáshoz és szerkesztéshez
    private ValidExpense validateExpense(User currentUser, ExpenseInput input) {
        if (input.title() == null || input.title().isBlank()) {
            throw new IllegalArgumentException("A megnevezés nem lehet üres!");
        }
        if (input.amount() == null || input.amount() <= 0) {
            throw new IllegalArgumentException("Az összegnek pozitívnak kell lennie!");
        }
        Currency currency = Currency.parse(input.currency());
        Category category = Category.parse(input.category());

        List<String> allUsers = getAllUsers();
        // Sima felhasználónál mindig ő a fizető (a kérésben küldött értéket figyelmen kívül hagyjuk),
        // az admin bárki nevében rögzíthet
        String payer = currentUser.isAdmin() ? input.payer() : currentUser.getName();
        if (payer == null || !allUsers.contains(payer)) {
            throw new IllegalArgumentException("Ismeretlen fizető: " + payer);
        }

        List<String> participants;
        if (input.participants() == null || input.participants().isEmpty()) {
            participants = new ArrayList<>(allUsers);
        } else {
            // Ismétlődő nevek kiszűrése, a sorrend megtartásával
            participants = new ArrayList<>(new LinkedHashSet<>(input.participants()));
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

        return new ValidExpense(input.title().trim(), input.amount(), currency, category, payer, participants);
    }
}
