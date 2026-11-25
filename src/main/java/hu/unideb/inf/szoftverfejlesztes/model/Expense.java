package hu.unideb.inf.szoftverfejlesztes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Melyik csoporthoz tartozik (a JSON-ban nem küldjük ki, a kliens úgyis tudja)
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

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

    // Megosztási mód; régi soroknál üres = egyenlő
    @Convert(converter = SplitType.JpaConverter.class)
    @Column(length = 10)
    private SplitType splitType;

    // Résztvevőnkénti részek. Régi soroknál üres: ilyenkor egyenlő megosztás a résztvevők között.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_shares", joinColumns = @JoinColumn(name = "expense_id"))
    @OrderColumn(name = "share_index")
    private List<ExpenseShare> shares = new ArrayList<>();

    // Ha ismétlődő szabályból jött létre, annak azonosítója
    private Long recurringExpenseId;

    // Rögzítés időpontja (szerkesztéskor nem változik); régi tételeknél üres lehet
    private LocalDateTime createdAt;

    // Ki rögzítette (felhasználónév); régi tételeknél üres, ezeket csak a tulajdonos / admin módosíthatja
    private String createdBy;

    // Egyenlő megosztású kiadás (a részek külön megadása nélkül)
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

    public SplitType getSplitType() {
        return splitType == null ? SplitType.EQUAL : splitType;
    }

    // Kinek mennyi jut a kiadásból. Ha nincsenek eltárolt részek (régi vagy egyszerű kiadás),
    // a résztvevők között egyenlően oszlik meg.
    @JsonIgnore
    public Map<String, Double> getShareAmounts() {
        Map<String, Double> result = new LinkedHashMap<>();
        if (shares != null && !shares.isEmpty()) {
            shares.forEach(share -> result.put(share.getPerson(), share.getAmount()));
        } else if (participants != null && !participants.isEmpty()) {
            double equalShare = amount / participants.size();
            participants.forEach(person -> result.put(person, equalShare));
        }
        return result;
    }
}
