package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "debts")
@Getter
@Setter
@NoArgsConstructor
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Column(nullable = false)
    private String debtor;

    @Column(nullable = false)
    private String creditor;

    @Column(nullable = false)
    private double amount;

    // Régi (oszlop előtti) soroknál üres, ezek forintot jelentenek
    @Convert(converter = Currency.JpaConverter.class)
    @Column(length = 10)
    private Currency currency;

    // Adatbázisba mentett nyers tartozásokhoz
    public Debt(Expense expense, String debtor, String creditor, double amount, Currency currency) {
        this.expense = expense;
        this.debtor = debtor;
        this.creditor = creditor;
        this.amount = amount;
        this.currency = currency;
    }

    // Optimalizált összesített egyenlegekhez (DTO-ként)
    public Debt(String debtor, String creditor, double amount, Currency currency) {
        this.debtor = debtor;
        this.creditor = creditor;
        this.amount = amount;
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency == null ? Currency.HUF : currency;
    }
}
