package hu.unideb.inf.szoftverfejlesztes.config;

import hu.unideb.inf.szoftverfejlesztes.model.Category;
import hu.unideb.inf.szoftverfejlesztes.model.Currency;
import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import hu.unideb.inf.szoftverfejlesztes.repository.DebtRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.ExpenseRepository;
import hu.unideb.inf.szoftverfejlesztes.repository.SettlementRepository;
import hu.unideb.inf.szoftverfejlesztes.services.BudgetService;
import hu.unideb.inf.szoftverfejlesztes.services.DebtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static hu.unideb.inf.szoftverfejlesztes.model.Category.*;
import static hu.unideb.inf.szoftverfejlesztes.model.Currency.*;

// Bemutató adatok: ha az adatbázisban még nincs egyetlen kiadás és rendezés sem, induláskor
// feltölti az elmúlt hónapok kiadásaival és néhány tartozás-rendezéssel (a statisztikához is).
// Kikapcsolható: kozoskassza.demo-data.enabled=false
@Component
@Order(2) // az alapértelmezett felhasználók (DefaultUsersInitializer) után fut
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String ADAM = "Ádám";
    private static final String DAVID = "Dávid";
    private static final String LEVI = "Levi";
    private static final String ZOLI = "Zoli";
    private static final List<String> TEAM = List.of(ADAM, DAVID, LEVI, ZOLI);
    // participants = null: mindenki között oszlik meg
    private static final List<String> ALL = null;

    private record DemoExpense(int daysAgo, int hour, String title, double amount, Currency currency,
                               Category category, String payer, List<String> participants) {
    }

    // Az adott napon a legnagyobb `count` darab tartozás teljes rendezése a megadott pénznemben
    private record DemoSettlement(int daysAgo, int hour, Currency currency, int count) {
    }

    private static final List<DemoExpense> EXPENSES = List.of(
            // Június
            new DemoExpense(165, 9, "Lakásbérlet – június", 240000, HUF, LAKAS, ZOLI, ALL),
            new DemoExpense(160, 18, "Rezsi (villany, gáz)", 38000, HUF, LAKAS, LEVI, ALL),
            new DemoExpense(155, 17, "Heti bevásárlás – Tesco", 24500, HUF, ETEL, ADAM, ALL),
            new DemoExpense(150, 20, "Pizza este", 12800, HUF, ETEL, DAVID, ALL),
            // Július
            new DemoExpense(135, 9, "Lakásbérlet – július", 240000, HUF, LAKAS, ZOLI, ALL),
            new DemoExpense(130, 7, "Balatoni vonatjegyek", 22400, HUF, UTAZAS, LEVI, ALL),
            new DemoExpense(128, 11, "Strandbelépő", 9600, HUF, SZORAKOZAS, ADAM, ALL),
            new DemoExpense(128, 14, "Lángos és fröccs", 7800, HUF, ETEL, DAVID, ALL),
            new DemoExpense(120, 10, "Internet-előfizetés", 8990, HUF, LAKAS, ZOLI, ALL),
            // Augusztus – bécsi kirándulás (euró)
            new DemoExpense(105, 9, "Lakásbérlet – augusztus", 240000, HUF, LAKAS, ZOLI, ALL),
            new DemoExpense(100, 15, "Szállás Bécsben (2 éj)", 360, EUR, LAKAS, DAVID, ALL),
            new DemoExpense(99, 11, "Bécsi múzeumjegyek", 68, EUR, SZORAKOZAS, LEVI, List.of(DAVID, LEVI, ZOLI)),
            new DemoExpense(99, 19, "Vacsora – bécsi szelet", 124.5, EUR, ETEL, ADAM, ALL),
            new DemoExpense(98, 8, "Vonatjegy Bécsbe", 96, EUR, UTAZAS, ZOLI, ALL),
            // Szeptember
            new DemoExpense(75, 9, "Lakásbérlet – szeptember", 240000, HUF, LAKAS, ZOLI, ALL),
            new DemoExpense(72, 16, "Tankönyvek", 18600, HUF, EGYEB, ADAM, List.of(ADAM, LEVI)),
            new DemoExpense(68, 20, "Mozi – IMAX", 11200, HUF, SZORAKOZAS, LEVI, ALL),
            new DemoExpense(62, 18, "Rezsi (szeptember)", 41500, HUF, LAKAS, LEVI, ALL),
            // Október – londoni út (font), előfizetés (dollár)
            new DemoExpense(45, 9, "Lakásbérlet – október", 240000, HUF, LAKAS, ZOLI, ALL),
            new DemoExpense(42, 13, "Repülőjegy Londonba", 220, GBP, UTAZAS, DAVID, List.of(DAVID, ZOLI)),
            new DemoExpense(40, 10, "Londoni metróbérlet", 48, GBP, UTAZAS, ZOLI, List.of(DAVID, ZOLI)),
            new DemoExpense(38, 21, "Steam játékcsomag", 39.99, USD, SZORAKOZAS, ADAM, List.of(ADAM, DAVID, LEVI)),
            new DemoExpense(35, 17, "Heti bevásárlás – Aldi", 27300, HUF, ETEL, DAVID, ALL),
            new DemoExpense(30, 19, "Szülinapi torta Levinek", 9500, HUF, ETEL, ZOLI, List.of(ADAM, DAVID, ZOLI)),
            // November
            new DemoExpense(17, 9, "Lakásbérlet – november", 240000, HUF, LAKAS, ZOLI, ALL),
            new DemoExpense(12, 12, "Takarítószerek", 6400, HUF, EGYEB, ADAM, ALL),
            new DemoExpense(8, 20, "Sushi vacsora", 21600, HUF, ETEL, LEVI, ALL),
            new DemoExpense(5, 18, "Koncertjegyek", 34000, HUF, SZORAKOZAS, DAVID, List.of(DAVID, LEVI)),
            new DemoExpense(2, 17, "Heti bevásárlás – Lidl", 19850, HUF, ETEL, ADAM, ALL),
            new DemoExpense(1, 21, "Netflix-előfizetés", 5490, HUF, SZORAKOZAS, ZOLI, ALL)
    );

    private static final List<DemoSettlement> SETTLEMENTS = List.of(
            new DemoSettlement(140, 12, HUF, 2),
            new DemoSettlement(110, 12, HUF, 2),
            new DemoSettlement(90, 18, EUR, 2),
            new DemoSettlement(55, 12, HUF, 2),
            new DemoSettlement(25, 10, GBP, 1),
            new DemoSettlement(14, 19, HUF, 1)
    );

    private final boolean enabled;
    private final ExpenseRepository expenseRepository;
    private final DebtRepository debtRepository;
    private final SettlementRepository settlementRepository;
    private final BudgetService budgetService;
    private final DebtService debtService;

    public DemoDataInitializer(@Value("${kozoskassza.demo-data.enabled:true}") boolean enabled,
                               ExpenseRepository expenseRepository, DebtRepository debtRepository,
                               SettlementRepository settlementRepository, BudgetService budgetService,
                               DebtService debtService) {
        this.enabled = enabled;
        this.expenseRepository = expenseRepository;
        this.debtRepository = debtRepository;
        this.settlementRepository = settlementRepository;
        this.budgetService = budgetService;
        this.debtService = debtService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || expenseRepository.count() > 0 || settlementRepository.count() > 0) {
            return;
        }
        if (!budgetService.getAllUsers().containsAll(TEAM)) {
            log.warn("Demó adatok kihagyva: nem létezik mind a négy csapattag felhasználó.");
            return;
        }

        // Időrendben haladunk, hogy a rendezések az akkori egyenlegeket egyenlítsék ki
        int settlementCount = 0;
        int maxDaysAgo = EXPENSES.stream().mapToInt(DemoExpense::daysAgo).max().orElse(0);
        for (int day = maxDaysAgo; day >= 0; day--) {
            for (DemoExpense demo : EXPENSES) {
                if (demo.daysAgo() == day) {
                    saveExpense(demo);
                }
            }
            for (DemoSettlement demo : SETTLEMENTS) {
                if (demo.daysAgo() == day) {
                    settlementCount += saveSettlements(demo);
                }
            }
        }
        log.info("Demó adatok betöltve: {} kiadás, {} rendezés.", EXPENSES.size(), settlementCount);
    }

    private void saveExpense(DemoExpense demo) {
        List<String> participants = demo.participants() == null ? new ArrayList<>(TEAM) : new ArrayList<>(demo.participants());
        Expense expense = new Expense(demo.title(), demo.amount(), demo.currency(), demo.category(),
                demo.payer(), participants, demo.payer());
        expense.setCreatedAt(at(demo.daysAgo(), demo.hour()));
        Expense saved = expenseRepository.save(expense);
        debtRepository.saveAll(debtService.calculateDebts(saved));
    }

    // A hitelező jelöli rendezettnek (ahogy az alkalmazásban is)
    private int saveSettlements(DemoSettlement demo) {
        List<Debt> largest = budgetService.getSimplifiedBalances().stream()
                .filter(d -> d.getCurrency() == demo.currency())
                .sorted(Comparator.comparingDouble(Debt::getAmount).reversed())
                .limit(demo.count())
                .toList();
        for (Debt debt : largest) {
            Settlement settlement = new Settlement(debt.getDebtor(), debt.getCreditor(), debt.getAmount(),
                    debt.getCurrency(), debt.getCreditor());
            settlement.setCreatedAt(at(demo.daysAgo(), demo.hour()));
            settlementRepository.save(settlement);
        }
        return largest.size();
    }

    private static LocalDateTime at(int daysAgo, int hour) {
        return LocalDateTime.now().minusDays(daysAgo).withHour(hour).withMinute(daysAgo % 60).withSecond(0).withNano(0);
    }
}
