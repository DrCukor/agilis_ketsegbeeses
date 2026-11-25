package hu.unideb.inf.szoftverfejlesztes.services;

import java.util.List;
import java.util.Map;

// Kiadás létrehozásához / módosításához beérkező adatok (még ellenőrzés előtt)
public record ExpenseInput(
        String title,
        Double amount,
        String currency,
        String category,
        String payer,              // csak a csoport tulajdonosa / admin adhatja meg, másnál ő maga a fizető
        List<String> participants, // üresen: a csoport minden tagja
        String splitType,          // EQUAL / PERCENT / AMOUNT / SHARES (üresen: EQUAL)
        Map<String, Double> splitValues // résztvevőnként: százalék / összeg / részarány
) {
}
