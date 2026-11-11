package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DebtService {

    private static final double EPSILON = 0.01;

    public List<Debt> calculateDebts(Expense expense) {
        List<Debt> debts = new ArrayList<>();
        List<String> participants = expense.getParticipants();
        String payer = expense.getPayer();
        double share = expense.getSharePerPerson();

        if (share <= 0) {
            return debts;
        }

        for (String participant : participants) {
            if (participant.equals(payer)) {
                continue;
            }
            debts.add(new Debt(expense, participant, payer, share));
        }
        return debts;
    }

    public List<Debt> simplifyDebts(List<Debt> rawDebts) {
        Map<String, Double> netBalances = new HashMap<>();

        // 1. Nettó egyenlegek összesítése személyenként
        for (Debt debt : rawDebts) {
            netBalances.merge(debt.getDebtor(), -debt.getAmount(), Double::sum);
            netBalances.merge(debt.getCreditor(), debt.getAmount(), Double::sum);
        }

        // 2. Szétválasztás adósokra és hitelezőkre tiszta belső objektumokkal
        List<PersonBalance> debtors = new ArrayList<>();
        List<PersonBalance> creditors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
            if (entry.getValue() < -EPSILON) {
                // Tartozás pozitív összegként rögzítve
                debtors.add(new PersonBalance(entry.getKey(), -entry.getValue()));
            } else if (entry.getValue() > EPSILON) {
                // Követelés pozitív összegként rögzítve
                creditors.add(new PersonBalance(entry.getKey(), entry.getValue()));
            }
        }

        // Rendezés a legnagyobb tételektől indulva (a minimális tranzakciószámhoz)
        debtors.sort(Comparator.comparingDouble((PersonBalance p) -> p.amount).reversed());
        creditors.sort(Comparator.comparingDouble((PersonBalance p) -> p.amount).reversed());

        // 3. Tranzakciók minimalizálása (mohó gráf-párosítás)
        List<Debt> simplifiedDebts = new ArrayList<>();
        int dIdx = 0;
        int cIdx = 0;

        while (dIdx < debtors.size() && cIdx < creditors.size()) {
            PersonBalance debtor = debtors.get(dIdx);
            PersonBalance creditor = creditors.get(cIdx);

            double settledAmount = Math.min(debtor.amount, creditor.amount);

            if (settledAmount > EPSILON) {
                simplifiedDebts.add(new Debt(debtor.name, creditor.name, Math.round(settledAmount)));
            }

            debtor.amount -= settledAmount;
            creditor.amount -= settledAmount;

            if (debtor.amount <= EPSILON) {
                dIdx++;
            }
            if (creditor.amount <= EPSILON) {
                cIdx++;
            }
        }

        return simplifiedDebts;
    }

    // Tiszta segédosztály a rendezéshez és az összegek módosításához
    private static class PersonBalance {
        String name;
        double amount;

        PersonBalance(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}