package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Category;
import hu.unideb.inf.szoftverfejlesztes.model.Currency;
import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.ExpenseGroup;
import hu.unideb.inf.szoftverfejlesztes.model.ExpenseShare;
import hu.unideb.inf.szoftverfejlesztes.model.RecurringExpense;
import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import hu.unideb.inf.szoftverfejlesztes.model.SplitType;
import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.DebtRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.SettlementRepository;
import hu.unideb.inf.szoftverfejlesztes.services.GroupService.GroupAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// Egy csoport kiadásai, tartozásai és rendezései. Minden jogosultsági szabály itt van
// (nem csak a felületen): a csoporthoz való hozzáférést a GroupService ellenőrzi.
@Service
public class BudgetService {

    private static final int MAX_TITLE_LENGTH = 100;

    private final ExpenseRepository expenseRepository;
    private final DebtRepository debtRepository;
    private final SettlementRepository settlementRepository;
    private final DebtService debtService;
    private final GroupService groupService;

    public BudgetService(ExpenseRepository expenseRepository, DebtRepository debtRepository,
                         SettlementRepository settlementRepository, DebtService debtService,
                         GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.debtRepository = debtRepository;
        this.settlementRepository = settlementRepository;
        this.debtService = debtService;
        this.groupService = groupService;
    }

    // ---------- Lekérdezések ----------

    public List<String> getMembers(User currentUser, Long groupId) {
        groupService.requireAccess(currentUser, groupId);
        return groupService.getMemberNames(groupId);
    }

    public List<Expense> getExpenses(User currentUser, Long groupId) {
        groupService.requireAccess(currentUser, groupId);
        return expenseRepository.findAllByGroupIdOrderByIdDesc(groupId);
    }

    public List<Debt> getBalances(User currentUser, Long groupId) {
        groupService.requireAccess(currentUser, groupId);
        return getGroupBalances(groupId);
    }

    public List<Settlement> getSettlements(User currentUser, Long groupId) {
        groupService.requireAccess(currentUser, groupId);
        return settlementRepository.findAllByGroupIdOrderByCreatedAtDescIdDesc(groupId);
    }

    // A csoport egyszerűsített egyenlegei: a nyers tartozások és a rendezések (fordított irányú
    // tartozásként) alapján, pénznemenként
    public List<Debt> getGroupBalances(Long groupId) {
        List<Debt> allDebts = new ArrayList<>(debtRepository.findAllByGroupId(groupId));
        for (Settlement settlement : settlementRepository.findAllByGroupIdOrderByCreatedAtDescIdDesc(groupId)) {
            allDebts.add(new Debt(settlement.getCreditor(), settlement.getDebtor(),
                    settlement.getAmount(), settlement.getCurrency()));
        }
        return debtService.simplifyDebts(allDebts);
    }

    // Egy személy egyenlege a csoportban pénznemenként: [0] = tartozása, [1] = ami neki jár
    public List<Map<Currency, Double>> getPersonalBalance(Long groupId, String person) {
        Map<Currency, Double> owe = new EnumMap<>(Currency.class);
        Map<Currency, Double> owed = new EnumMap<>(Currency.class);
        for (Debt debt : getGroupBalances(groupId)) {
            if (debt.getDebtor().equals(person)) {
                owe.merge(debt.getCurrency(), debt.getAmount(), Double::sum);
            }
            if (debt.getCreditor().equals(person)) {
                owed.merge(debt.getCurrency(), debt.getAmount(), Double::sum);
            }
        }
        return List.of(owe, owed);
    }

    public boolean hasOpenBalance(Long groupId, String person) {
        return getGroupBalances(groupId).stream()
                .anyMatch(d -> d.getDebtor().equals(person) || d.getCreditor().equals(person));
    }

    // ---------- Kiadások ----------

    @Transactional
    public Expense createExpense(User currentUser, Long groupId, ExpenseInput input) {
        return createExpense(currentUser, groupId, input, null);
    }

    // recurringExpenseId: ha egy ismétlődő szabály első példánya
    @Transactional
    public Expense createExpense(User currentUser, Long groupId, ExpenseInput input, Long recurringExpenseId) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        PreparedExpense prepared = prepareExpense(access, input);
        return persistExpense(access.group(), prepared, currentUser.getName(), LocalDateTime.now(), recurringExpenseId);
    }

    @Transactional
    public Expense updateExpense(User currentUser, Long groupId, Long expenseId, ExpenseInput input) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        Expense expense = findExpense(groupId, expenseId);
        checkCanModify(access, expense);
        PreparedExpense prepared = prepareExpense(access, input);

        expense.setTitle(prepared.title());
        expense.setAmount(prepared.amount());
        expense.setCurrency(prepared.currency());
        expense.setCategory(prepared.category());
        expense.setPayer(prepared.payer());
        expense.setSplitType(prepared.splitType());
        expense.setParticipants(new ArrayList<>(prepared.participants()));
        expense.getShares().clear();
        expense.getShares().addAll(prepared.shares());
        Expense savedExpense = expenseRepository.save(expense);

        // A régi nyers tartozások helyett újakat számolunk a módosított adatokból
        debtRepository.deleteByExpenseId(expenseId);
        debtRepository.saveAll(debtService.calculateDebts(savedExpense));
        return savedExpense;
    }

    @Transactional
    public void deleteExpense(User currentUser, Long groupId, Long expenseId) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        Expense expense = findExpense(groupId, expenseId);
        checkCanModify(access, expense);
        debtRepository.deleteByExpenseId(expenseId);
        expenseRepository.delete(expense);
    }

    // Ismétlődő szabályból automatikusan létrehozott kiadás (nincs bejelentkezett felhasználó):
    // a szabály adataiból, a csoport aktuális tagjaira ellenőrizve.
    // Nem @Transactional: az ellenőrzési hiba így nem jelöli visszagörgetésre a hívó tranzakcióját.
    public Expense createFromRecurring(RecurringExpense rule, LocalDate date) {
        ExpenseGroup group = rule.getGroup();
        List<String> members = groupService.getMemberNames(group.getId());
        if (!members.contains(rule.getPayer())) {
            throw new IllegalArgumentException("A fizető már nem tagja a csoportnak: " + rule.getPayer());
        }
        List<String> participants = rule.getShares().stream().map(ExpenseShare::getPerson).toList();
        for (String participant : participants) {
            if (!members.contains(participant)) {
                throw new IllegalArgumentException("A résztvevő már nem tagja a csoportnak: " + participant);
            }
        }
        Map<String, Double> values = new LinkedHashMap<>();
        rule.getShares().forEach(s -> values.put(s.getPerson(), s.getInputValue()));
        List<ExpenseShare> shares = ShareCalculator.calculate(rule.getSplitType(), participants, values,
                rule.getAmount(), rule.getCurrency());

        PreparedExpense prepared = new PreparedExpense(rule.getTitle(), rule.getAmount(), rule.getCurrency(),
                rule.getCategory(), rule.getPayer(), rule.getSplitType(), shares);
        return persistExpense(group, prepared, rule.getCreatedBy(), date.atTime(LocalTime.of(9, 0)), rule.getId());
    }

    private Expense persistExpense(ExpenseGroup group, PreparedExpense prepared, String createdBy,
                                   LocalDateTime createdAt, Long recurringExpenseId) {
        Expense expense = new Expense(prepared.title(), prepared.amount(), prepared.currency(), prepared.category(),
                prepared.payer(), new ArrayList<>(prepared.participants()), createdBy);
        expense.setGroup(group);
        expense.setSplitType(prepared.splitType());
        expense.setShares(new ArrayList<>(prepared.shares()));
        expense.setCreatedAt(createdAt);
        expense.setRecurringExpenseId(recurringExpenseId);
        Expense savedExpense = expenseRepository.save(expense);

        debtRepository.saveAll(debtService.calculateDebts(savedExpense));
        return savedExpense;
    }

    // ---------- Rendezések ----------

    // Az adós -> hitelező tartozás teljes kiegyenlítése a megadott pénznemben
    // (ha nincs megadva pénznem, akkor mindegyikben).
    // Csak a hitelező jelölheti (ő tudja, hogy megkapta a pénzt), illetve a tulajdonos / admin.
    @Transactional
    public List<Settlement> settleDebt(User currentUser, Long groupId, String debtor, String creditor, String currencyCode) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        if (!access.canManage() && !currentUser.getName().equals(creditor)) {
            throw new ForbiddenException("Csak a hitelező jelölheti a tartozást rendezettnek!");
        }
        Currency currency = currencyCode == null || currencyCode.isBlank() ? null : Currency.parse(currencyCode);

        List<Debt> balances = getGroupBalances(groupId).stream()
                .filter(d -> d.getDebtor().equals(debtor) && d.getCreditor().equals(creditor))
                .filter(d -> currency == null || d.getCurrency() == currency)
                .toList();
        if (balances.isEmpty()) {
            String currencyText = currency == null ? "" : " " + currency.name();
            throw new IllegalArgumentException("Nincs rendezendő" + currencyText + " tartozás: " + debtor + " -> " + creditor);
        }

        return balances.stream()
                .map(b -> {
                    Settlement settlement = new Settlement(debtor, creditor, b.getAmount(), b.getCurrency(), currentUser.getName());
                    settlement.setGroup(access.group());
                    return settlementRepository.save(settlement);
                })
                .toList();
    }

    // ---------- Törlés ----------

    // A csoport összes kiadásának, tartozásának és rendezésének törlése (tulajdonos / admin)
    @Transactional
    public void deleteGroupData(User currentUser, Long groupId) {
        GroupAccess access = groupService.requireAccess(currentUser, groupId);
        if (!access.canManage()) {
            throw new ForbiddenException("A csoport adatait csak a tulajdonos törölheti!");
        }
        deleteGroupDataInternal(groupId);
    }

    // Jogosultság-ellenőrzés nélkül (csoport törlésekor hívjuk)
    @Transactional
    public void deleteGroupDataInternal(Long groupId) {
        settlementRepository.deleteAll(settlementRepository.findAllByGroupIdOrderByCreatedAtDescIdDesc(groupId));
        debtRepository.deleteAllByGroupId(groupId);
        // Egyenként töröljük, hogy a résztvevők és a részek is törlődjenek
        expenseRepository.deleteAll(expenseRepository.findAllByGroupIdOrderByIdDesc(groupId));
    }

    // Rendszer-admin: minden csoport összes kiadásának, tartozásának és rendezésének törlése
    @Transactional
    public void deleteAllData(User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Az összes adatot csak az admin törölheti!");
        }
        settlementRepository.deleteAllInBatch();
        debtRepository.deleteAllInBatch();
        // Egyenként töröljük, hogy a résztvevők és a részek is törlődjenek
        expenseRepository.deleteAll();
    }

    // ---------- Ellenőrzés ----------

    private Expense findExpense(Long groupId, Long expenseId) {
        return expenseRepository.findById(expenseId)
                .filter(e -> e.getGroup() != null && e.getGroup().getId().equals(groupId))
                .orElseThrow(() -> new NoSuchElementException("Nem található kiadás: " + expenseId));
    }

    // Csak a saját rögzítésű kiadás módosítható / törölhető; a tulajdonos és az admin bármelyiket.
    // A createdBy nélküli (régi) tételeket csak a tulajdonos / admin kezelheti.
    private void checkCanModify(GroupAccess access, Expense expense) {
        if (access.canManage()) {
            return;
        }
        if (expense.getCreatedBy() == null || !expense.getCreatedBy().equals(access.user().getName())) {
            throw new ForbiddenException("Csak a saját kiadásaidat módosíthatod vagy törölheted!");
        }
    }

    // Ellenőrzött, mentésre kész kiadás-adatok (a részek már kiszámolva)
    public record PreparedExpense(String title, double amount, Currency currency, Category category,
                                  String payer, SplitType splitType, List<ExpenseShare> shares) {
        public List<String> participants() {
            return shares.stream().map(ExpenseShare::getPerson).toList();
        }
    }

    // Közös ellenőrzés létrehozáshoz, szerkesztéshez és ismétlődő szabályhoz
    public PreparedExpense prepareExpense(GroupAccess access, ExpenseInput input) {
        if (input.title() == null || input.title().isBlank()) {
            throw new IllegalArgumentException("A megnevezés nem lehet üres!");
        }
        // Túl hosszú szövegnél az adatbázis hibát dobna (500), ezért előre, érthető üzenettel jelezzük
        if (input.title().trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("A megnevezés legfeljebb " + MAX_TITLE_LENGTH + " karakter lehet!");
        }
        Currency currency = Currency.parse(input.currency());
        if (input.amount() == null || input.amount() <= 0) {
            throw new IllegalArgumentException("Az összegnek pozitívnak kell lennie!");
        }
        // A pénznem legkisebb egységére kerekítjük (forintnál egész szám)
        double amount = ShareCalculator.round(input.amount(), currency);
        if (amount <= 0) {
            throw new IllegalArgumentException("Az összegnek pozitívnak kell lennie!");
        }
        Category category = Category.parse(input.category());
        SplitType splitType = SplitType.parse(input.splitType());

        List<String> members = groupService.getMemberNames(access.group().getId());
        // Sima tagnál mindig ő a fizető (a kérésben küldött értéket figyelmen kívül hagyjuk),
        // a tulajdonos és az admin bárki nevében rögzíthet
        String payer = access.canManage() ? input.payer() : access.user().getName();
        if (payer == null || !members.contains(payer)) {
            throw new IllegalArgumentException("Ismeretlen fizető: " + payer);
        }

        List<String> participants;
        if (input.participants() == null || input.participants().isEmpty()) {
            participants = new ArrayList<>(members);
        } else {
            // Ismétlődő nevek kiszűrése, a sorrend megtartásával
            participants = new ArrayList<>(new LinkedHashSet<>(input.participants()));
            for (String participant : participants) {
                if (!members.contains(participant)) {
                    throw new IllegalArgumentException("Ismeretlen résztvevő: " + participant);
                }
            }
        }

        List<ExpenseShare> shares = ShareCalculator.calculate(splitType, participants, input.splitValues(), amount, currency);
        // Ha csak maga a fizető kap részt, nem keletkezne tartozás
        if (shares.stream().allMatch(s -> s.getPerson().equals(payer))) {
            throw new IllegalArgumentException("Legalább egy résztvevőnek a fizetőn kívül kell lennie!");
        }
        return new PreparedExpense(input.title().trim(), amount, currency, category, payer, splitType, shares);
    }
}
