package hu.unideb.inf.szoftverfejlesztes.services;

import java.util.List;

// Kiadás létrehozásához / módosításához beérkező adatok (még ellenőrzés előtt)
public record ExpenseInput(
        String title,
        Double amount,
        String currency,
        String category,
        String payer,           // csak az admin adhatja meg, másnál a bejelentkezett felhasználó a fizető
        List<String> participants
) {
}
