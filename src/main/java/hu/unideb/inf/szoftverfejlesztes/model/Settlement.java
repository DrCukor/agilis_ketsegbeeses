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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Settlement(String debtor, String creditor, double amount) {
        this.debtor = debtor;
        this.creditor = creditor;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }
}
