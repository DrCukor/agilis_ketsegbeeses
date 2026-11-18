package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Tartozás kiegyenlítése: az adós (debtor) kifizette a hitelezőnek (creditor) az összeget
@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Ki jelölte rendezettnek (felhasználónév); régi soroknál üres
    private String settledBy;

    public Settlement(String debtor, String creditor, double amount, Currency currency, String settledBy) {
        this.debtor = debtor;
        this.creditor = creditor;
        this.amount = amount;
        this.currency = currency;
        this.settledBy = settledBy;
        this.createdAt = LocalDateTime.now();
    }

    public Currency getCurrency() {
        return currency == null ? Currency.HUF : currency;
    }
}
