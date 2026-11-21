package hu.unideb.inf.szoftverfejlesztes.services;
import hu.unideb.inf.szoftverfejlesztes.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class DebtServiceTest {
    private DebtService debtService;

    @BeforeEach
    void setUp() {
        debtService = new DebtService();
    }
    @Test
    void calculateDebt_SouldSplitEqually(){
        Expense expense = new Expense("Jegy",10000,Currency.HUF,Category.UTAZAS,"Ádám", List.of("Zoli","Levi","Dávid","Ádám"),
                "Ádám");
        List<Debt>debts = debtService.calculateDebts(expense);
        assertEquals(3,debts.size());
        assertTrue(debts.stream().anyMatch((debt -> {return debt.getDebtor().equals("Zoli") && debt.getCreditor().equals("Ádám")
                && debt.getAmount() == 2500;})));
        assertTrue(debts.stream().anyMatch((debt -> {return debt.getDebtor().equals("Levi") && debt.getCreditor().equals("Ádám")
                && debt.getAmount() == 2500;})));
        assertTrue(debts.stream().anyMatch((debt -> {return debt.getDebtor().equals("Dávid") && debt.getCreditor().equals("Ádám")
                && debt.getAmount() == 2500;})));
    }
    @Test
    void calculateDebts_shouldNotCreateDebtForPayer() {
        Expense expense = new Expense(
                "Bevásárlás",
                6000,
                Currency.HUF,
                Category.ETEL,
                "Ádám",
                List.of("Ádám", "Dávid"),
                "Ádám"
        );

        List<Debt> debts = debtService.calculateDebts(expense);

        assertEquals(1, debts.size());
        assertEquals("Dávid", debts.get(0).getDebtor());
        assertEquals("Ádám", debts.get(0).getCreditor());
        assertEquals(3000, debts.get(0).getAmount(), 0.001);
    }

    @Test
    void calculateDebts_shouldReturnEmptyForNoParticipants() {
        Expense expense = new Expense(
                "Teszt",
                5000,
                Currency.HUF,
                Category.EGYEB,
                "Ádám",
                List.of(),
                "Ádám"
        );

        List<Debt> debts = debtService.calculateDebts(expense);

        assertTrue(debts.isEmpty());
    }

    @Test
    void simplifyDebts_shouldCancelOppositeDebts() {
        List<Debt> rawDebts = List.of(
                new Debt("Ádám", "Dávid", 5000, Currency.HUF),
                new Debt("Dávid", "Ádám", 3000, Currency.HUF)
        );

        List<Debt> result = debtService.simplifyDebts(rawDebts);

        assertEquals(1, result.size());
        assertEquals("Ádám", result.get(0).getDebtor());
        assertEquals("Dávid", result.get(0).getCreditor());
        assertEquals(2000, result.get(0).getAmount(), 0.001);
    }

    @Test
    void simplifyDebts_shouldKeepCurrenciesSeparate() {
        List<Debt> rawDebts = List.of(
                new Debt("Ádám", "Dávid", 5000, Currency.HUF),
                new Debt("Dávid", "Ádám", 20, Currency.EUR)
        );

        List<Debt> result = debtService.simplifyDebts(rawDebts);

        assertEquals(2, result.size());

        assertTrue(result.stream().anyMatch(d ->
                d.getCurrency() == Currency.HUF
                        && d.getAmount() == 5000
        ));

        assertTrue(result.stream().anyMatch(d ->
                d.getCurrency() == Currency.EUR
                        && d.getAmount() == 20
        ));
    }
}
