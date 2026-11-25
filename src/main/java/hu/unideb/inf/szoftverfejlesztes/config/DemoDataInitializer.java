package hu.unideb.inf.szoftverfejlesztes.config;

import hu.unideb.inf.szoftverfejlesztes.model.Category;
import hu.unideb.inf.szoftverfejlesztes.model.Currency;
import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.ExpenseGroup;
import hu.unideb.inf.szoftverfejlesztes.model.ExpenseShare;
import hu.unideb.inf.szoftverfejlesztes.model.RecurringExpense;
import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import hu.unideb.inf.szoftverfejlesztes.model.SplitType;
import hu.unideb.inf.szoftverfejlesztes.repository.DebtRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseGroupRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.RecurringExpenseRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.SettlementRepository;
import hu.unideb.inf.szoftverfejlesztes.services.BudgetService;
import hu.unideb.inf.szoftverfejlesztes.services.DebtService;
import hu.unideb.inf.szoftverfejlesztes.services.GroupService;
import hu.unideb.inf.szoftverfejlesztes.services.ShareCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static hu.unideb.inf.szoftverfejlesztes.model.Category.*;
import static hu.unideb.inf.szoftverfejlesztes.model.Currency.*;

// Bemutató adatok: ha az adatbázisban még nincs egyetlen kiadás és rendezés sem, induláskor
// feltölti az alapcsoportot az elmúlt hónapok kiadásaival (egyéni megosztásokkal is), két havonta
// ismétlődő kiadással (lakbér, Netflix) és néhány tartozás-rendezéssel.
// Kikapcsolható: kozoskassza.demo-data.enabled=false
@Component
@Order(3) // a felhasználók és az alapcsoport létrehozása után fut
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String ADAM = "Ádám";
    private static final String DAVID = "Dávid";
    private static final String LEVI = "Levi";
    private static final String ZOLI = "Zoli";
    private static final List<String> TEAM = List.of(ADAM, DAVID, LEVI, ZOLI);

    private static final int RENT_DAY = 1;
    private static final int NETFLIX_DAY = 24;
    // A lakbér megosztása: Zolinak a legnagyobb a szobája
    private static final Map<String, Double> RENT_PERCENTS = Map.of(ZOLI, 30.0, ADAM, 25.0, DAVID, 25.0, LEVI, 20.0);

    private record DemoExpense(LocalDateTime time, String title, double amount, Currency currency, Category category,
                               String payer, List<String> participants, SplitType splitType,
                               Map<String, Double> values, Long recurringId) {
    }

    // Az adott időpontban a legnagyobb `count` darab tartozás teljes rendezése a megadott pénznemben
    private record DemoSettlement(LocalDateTime time, Currency currency, int count) {
    }

    private final boolean enabled;
    private final ExpenseGroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final DebtRepository debtRepository;
    private final SettlementRepository settlementRepository;
    private final RecurringExpenseRepository recurringRepository;
    private final BudgetService budgetService;
    private final DebtService debtService;
    private final GroupService groupService;

    public DemoDataInitializer(@Value("${kozoskassza.demo-data.enabled:true}") boolean enabled,
                               ExpenseGroupRepository groupRepository, ExpenseRepository expenseRepository,
                               DebtRepository debtRepository, SettlementRepository settlementRepository,
                               RecurringExpenseRepository recurringRepository, BudgetService budgetService,
                               DebtService debtService, GroupService groupService) {
        this.enabled = enabled;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.debtRepository = debtRepository;
        this.settlementRepository = settlementRepository;
        this.recurringRepository = recurringRepository;
        this.budgetService = budgetService;
        this.debtService = debtService;
        this.groupService = groupService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || expenseRepository.count() > 0 || settlementRepository.count() > 0) {
            return;
        }
        ExpenseGroup group = groupRepository.findAll(Sort.by("id")).stream().findFirst().orElse(null);
        if (group == null || !groupService.getMemberNames(group.getId()).containsAll(TEAM)) {
            log.warn("Demó adatok kihagyva: nincs olyan csoport, amelyben mind a négy csapattag benne van.");
            return;
        }

        LocalDate today = LocalDate.now();
        RecurringExpense rent = saveRecurring(group, "Lakásbérlet", 240000, HUF, LAKAS, ZOLI,
                SplitType.PERCENT, RENT_PERCENTS, RENT_DAY, today);
        RecurringExpense netflix = saveRecurring(group, "Netflix-előfizetés", 5490, HUF, SZORAKOZAS, ZOLI,
                SplitType.EQUAL, null, NETFLIX_DAY, today);

        List<DemoExpense> expenses = new ArrayList<>(oneOffExpenses());
        // Az ismétlődő kiadások korábbi hónapjai (az elmúlt öt hónap és a mostani)
        for (int monthsAgo = 5; monthsAgo >= 0; monthsAgo--) {
            LocalDate month = today.minusMonths(monthsAgo);
            LocalDate rentDate = month.withDayOfMonth(RENT_DAY);
            LocalDate netflixDate = month.withDayOfMonth(NETFLIX_DAY);
            if (!rentDate.isAfter(today)) {
                expenses.add(new DemoExpense(rentDate.atTime(9, 0), "Lakásbérlet", 240000, HUF, LAKAS, ZOLI,
                        TEAM, SplitType.PERCENT, RENT_PERCENTS, rent.getId()));
            }
            if (!netflixDate.isAfter(today)) {
                expenses.add(new DemoExpense(netflixDate.atTime(9, 0), "Netflix-előfizetés", 5490, HUF, SZORAKOZAS, ZOLI,
                        TEAM, SplitType.EQUAL, null, netflix.getId()));
            }
        }

        List<DemoSettlement> settlements = List.of(
                new DemoSettlement(daysAgo(140, 12), HUF, 2),
                new DemoSettlement(daysAgo(110, 12), HUF, 2),
                new DemoSettlement(daysAgo(90, 18), EUR, 2),
                new DemoSettlement(daysAgo(55, 12), HUF, 2),
                new DemoSettlement(daysAgo(25, 10), GBP, 1),
                new DemoSettlement(daysAgo(14, 19), HUF, 1)
        );

        // Időrendben haladunk, hogy a rendezések az akkori egyenlegeket egyenlítsék ki
        expenses.sort(Comparator.comparing(DemoExpense::time));
        int expenseIndex = 0;
        int settlementCount = 0;
        for (DemoSettlement settlement : settlements) {
            while (expenseIndex < expenses.size() && expenses.get(expenseIndex).time().isBefore(settlement.time())) {
                saveExpense(group, expenses.get(expenseIndex++));
            }
            settlementCount += saveSettlements(group, settlement);
        }
        while (expenseIndex < expenses.size()) {
            saveExpense(group, expenses.get(expenseIndex++));
        }
        log.info("Demó adatok betöltve: {} kiadás, {} rendezés, 2 ismétlődő kiadás.", expenses.size(), settlementCount);
    }

    private static List<DemoExpense> oneOffExpenses() {
        return List.of(
                // Június
                equal(160, 18, "Rezsi (villany, gáz)", 38000, HUF, LAKAS, LEVI, TEAM),
                equal(155, 17, "Heti bevásárlás – Tesco", 24500, HUF, ETEL, ADAM, TEAM),
                equal(150, 20, "Pizza este", 12800, HUF, ETEL, DAVID, TEAM),
                // Július
                equal(130, 7, "Balatoni vonatjegyek", 22400, HUF, UTAZAS, LEVI, TEAM),
                equal(128, 11, "Strandbelépő", 9600, HUF, SZORAKOZAS, ADAM, TEAM),
                custom(128, 14, "Lángos és fröccs", 7800, HUF, ETEL, DAVID, SplitType.SHARES,
                        values(DAVID, 1, ADAM, 1, LEVI, 2, ZOLI, 1)),
                equal(120, 10, "Internet-előfizetés", 8990, HUF, LAKAS, ZOLI, TEAM),
                // Augusztus – bécsi kirándulás (euró)
                equal(100, 15, "Szállás Bécsben (2 éj)", 360, EUR, LAKAS, DAVID, TEAM),
                equal(99, 11, "Bécsi múzeumjegyek", 68, EUR, SZORAKOZAS, LEVI, List.of(DAVID, LEVI, ZOLI)),
                custom(99, 19, "Vacsora – bécsi szelet", 124.5, EUR, ETEL, ADAM, SplitType.AMOUNT,
                        values(ADAM, 34.5, DAVID, 30, LEVI, 28, ZOLI, 32)),
                equal(98, 8, "Vonatjegy Bécsbe", 96, EUR, UTAZAS, ZOLI, TEAM),
                // Szeptember
                equal(72, 16, "Tankönyvek", 18600, HUF, EGYEB, ADAM, List.of(ADAM, LEVI)),
                equal(68, 20, "Mozi – IMAX", 11200, HUF, SZORAKOZAS, LEVI, TEAM),
                equal(62, 18, "Rezsi (szeptember)", 41500, HUF, LAKAS, LEVI, TEAM),
                // Október – londoni út (font), előfizetés (dollár)
                equal(42, 13, "Repülőjegy Londonba", 220, GBP, UTAZAS, DAVID, List.of(DAVID, ZOLI)),
                equal(40, 10, "Londoni metróbérlet", 48, GBP, UTAZAS, ZOLI, List.of(DAVID, ZOLI)),
                equal(38, 21, "Steam játékcsomag", 39.99, USD, SZORAKOZAS, ADAM, List.of(ADAM, DAVID, LEVI)),
                equal(35, 17, "Heti bevásárlás – Aldi", 27300, HUF, ETEL, DAVID, TEAM),
                equal(30, 19, "Szülinapi torta Levinek", 9500, HUF, ETEL, ZOLI, List.of(ADAM, DAVID, ZOLI)),
                // November
                equal(12, 12, "Takarítószerek", 6400, HUF, EGYEB, ADAM, TEAM),
                custom(8, 20, "Sushi vacsora", 21600, HUF, ETEL, LEVI, SplitType.SHARES,
                        values(LEVI, 2, ADAM, 1, DAVID, 1, ZOLI, 1)),
                custom(5, 18, "Koncertjegyek", 34000, HUF, SZORAKOZAS, DAVID, SplitType.AMOUNT,
                        values(DAVID, 20000, LEVI, 14000)),
                custom(2, 17, "Heti bevásárlás – Lidl", 19850, HUF, ETEL, ADAM, SplitType.PERCENT,
                        values(ADAM, 40, DAVID, 20, LEVI, 20, ZOLI, 20))
        );
    }

    private RecurringExpense saveRecurring(ExpenseGroup group, String title, double amount, Currency currency,
                                           Category category, String payer, SplitType splitType,
                                           Map<String, Double> values, int day, LocalDate today) {
        RecurringExpense rule = new RecurringExpense();
        rule.setGroup(group);
        rule.setTitle(title);
        rule.setAmount(amount);
        rule.setCurrency(currency);
        rule.setCategory(category);
        rule.setPayer(payer);
        rule.setSplitType(splitType);
        rule.setShares(new ArrayList<>(ShareCalculator.calculate(splitType, TEAM, values, amount, currency)));
        rule.setDayOfMonth(day);
        // A következő esedékesség mindig a jövőben van (a korábbi hónapokat a demó maga hozza létre)
        LocalDate thisMonth = today.withDayOfMonth(day);
        rule.setNextRunDate(thisMonth.isAfter(today) ? thisMonth : thisMonth.plusMonths(1));
        rule.setCreatedBy(payer);
        rule.setCreatedAt(today.minusMonths(5).withDayOfMonth(1).atStartOfDay());
        return recurringRepository.save(rule);
    }

    private void saveExpense(ExpenseGroup group, DemoExpense demo) {
        List<ExpenseShare> shares = ShareCalculator.calculate(demo.splitType(), demo.participants(), demo.values(),
                demo.amount(), demo.currency());
        List<String> participants = new ArrayList<>(shares.stream().map(ExpenseShare::getPerson).toList());
        Expense expense = new Expense(demo.title(), demo.amount(), demo.currency(), demo.category(),
                demo.payer(), participants, demo.payer());
        expense.setGroup(group);
        expense.setSplitType(demo.splitType());
        expense.setShares(new ArrayList<>(shares));
        expense.setCreatedAt(demo.time());
        expense.setRecurringExpenseId(demo.recurringId());
        Expense saved = expenseRepository.save(expense);
        debtRepository.saveAll(debtService.calculateDebts(saved));
    }

    // A hitelező jelöli rendezettnek (ahogy az alkalmazásban is)
    private int saveSettlements(ExpenseGroup group, DemoSettlement demo) {
        List<Debt> largest = budgetService.getGroupBalances(group.getId()).stream()
                .filter(d -> d.getCurrency() == demo.currency())
                .sorted(Comparator.comparingDouble(Debt::getAmount).reversed())
                .limit(demo.count())
                .toList();
        for (Debt debt : largest) {
            Settlement settlement = new Settlement(debt.getDebtor(), debt.getCreditor(), debt.getAmount(),
                    debt.getCurrency(), debt.getCreditor());
            settlement.setGroup(group);
            settlement.setCreatedAt(demo.time());
            settlementRepository.save(settlement);
        }
        return largest.size();
    }

    private static DemoExpense equal(int daysAgo, int hour, String title, double amount, Currency currency,
                                     Category category, String payer, List<String> participants) {
        return new DemoExpense(daysAgo(daysAgo, hour), title, amount, currency, category, payer, participants,
                SplitType.EQUAL, null, null);
    }

    private static DemoExpense custom(int daysAgo, int hour, String title, double amount, Currency currency,
                                      Category category, String payer, SplitType splitType, Map<String, Double> values) {
        return new DemoExpense(daysAgo(daysAgo, hour), title, amount, currency, category, payer,
                new ArrayList<>(values.keySet()), splitType, values, null);
    }

    // Név-érték párokból sorrendtartó map, pl. values("Ádám", 2, "Dávid", 1)
    private static Map<String, Double> values(Object... pairs) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put((String) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
        return result;
    }

    private static LocalDateTime daysAgo(int daysAgo, int hour) {
        return LocalDate.now().minusDays(daysAgo).atTime(LocalTime.of(hour, daysAgo % 60));
    }
}
