package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Currency;
import hu.unideb.inf.szoftverfejlesztes.model.Debt;
import hu.unideb.inf.szoftverfejlesztes.model.Expense;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DebtService {

    // Minden résztvevő (a fizetőn kívül) a rá eső résszel tartozik a fizetőnek.
    // A részeket az Expense.getShareAmounts() adja (egyéni megosztás, vagy egyenlő elosztás).
    public List<Debt> calculateDebts(Expense expense) {
        List<Debt> debts = new ArrayList<>();
        String payer = expense.getPayer();

        expense.getShareAmounts().forEach((participant, share) -> {
            if (!participant.equals(payer) && share > 0) {
                debts.add(new Debt(expense, participant, payer, share, expense.getCurrency()));
            }
        });
        return debts;
    }

    // Pénznemenként külön egyszerűsít: a forintos és az eurós tartozások nem nettósítják egymást
    public List<Debt> simplifyDebts(List<Debt> rawDebts) {
        Map<Currency, List<Debt>> debtsByCurrency = new EnumMap<>(Currency.class);
        for (Debt debt : rawDebts) {
            debtsByCurrency.computeIfAbsent(debt.getCurrency(), c -> new ArrayList<>()).add(debt);
        }

        List<Debt> simplifiedDebts = new ArrayList<>();
        debtsByCurrency.forEach((currency, debts) -> simplifiedDebts.addAll(simplifyInCurrency(debts, currency)));
        return simplifiedDebts;
    }

    private List<Debt> simplifyInCurrency(List<Debt> rawDebts, Currency currency) {
        // Az ennél kisebb maradékot rendezettnek tekintjük (pl. fél forint, fél cent)
        double epsilon = currency.getEpsilon();
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
            if (entry.getValue() < -epsilon) {
                // Tartozás pozitív összegként rögzítve
                debtors.add(new PersonBalance(entry.getKey(), -entry.getValue()));
            } else if (entry.getValue() > epsilon) {
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

            // Pontos (nem kerekített) összeg, hogy a kiegyenlítés maradék nélkül nullázzon;
            // a kerekítés csak a megjelenítéskor történik
            if (settledAmount > epsilon) {
                simplifiedDebts.add(new Debt(debtor.name, creditor.name, settledAmount, currency));
            }

            debtor.amount -= settledAmount;
            creditor.amount -= settledAmount;

            if (debtor.amount <= epsilon) {
                dIdx++;
            }
            if (creditor.amount <= epsilon) {
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
