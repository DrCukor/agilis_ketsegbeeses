package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private double amount;

    // Régi (oszlop előtti) soroknál üres, ezek forintot jelentenek
    @Convert(converter = Currency.JpaConverter.class)
    @Column(length = 10)
    private Currency currency;

    // Régi (oszlop előtti) soroknál üres, ezek "Egyéb" kategóriát jelentenek
    @Convert(converter = Category.JpaConverter.class)
    @Column(length = 20)
    private Category category;

    @Column(nullable = false)
    private String payer;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_participants", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "person")
    private List<String> participants = new ArrayList<>();

    // Rögzítés időpontja (szerkesztéskor nem változik); régi tételeknél üres lehet
    private LocalDateTime createdAt;

    // Ki rögzítette (felhasználónév); régi tételeknél üres, ezeket csak az admin módosíthatja
    private String createdBy;

    public Expense(String title, double amount, Currency currency, Category category,
                   String payer, List<String> participants, String createdBy) {
        this.title = title;
        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.payer = payer;
        this.participants = participants;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Currency getCurrency() {
        return currency == null ? Currency.HUF : currency;
    }

    public Category getCategory() {
        return category == null ? Category.EGYEB : category;
    }

    public double getSharePerPerson() {
        if (participants == null || participants.isEmpty()) {
            return 0.0;
        }
        return amount / participants.size();
    }
}
