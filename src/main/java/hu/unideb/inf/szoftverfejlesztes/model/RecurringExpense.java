package hu.unideb.inf.szoftverfejlesztes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Havonta ismétlődő kiadás szabálya (pl. lakbér, Netflix): minden hónap adott napján
// automatikusan létrejön belőle egy kiadás
@Entity
@Table(name = "recurring_expenses")
@Getter
@Setter
@NoArgsConstructor
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private double amount;

    @Convert(converter = Currency.JpaConverter.class)
    @Column(length = 10)
    private Currency currency;

    @Convert(converter = Category.JpaConverter.class)
    @Column(length = 20)
    private Category category;

    @Column(nullable = false)
    private String payer;

    @Convert(converter = SplitType.JpaConverter.class)
    @Column(length = 10)
    private SplitType splitType;

    // A résztvevők és a megadott értékek (százalék / összeg / részarány); az összegeket minden
    // létrehozáskor újraszámoljuk
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recurring_expense_shares", joinColumns = @JoinColumn(name = "recurring_expense_id"))
    @OrderColumn(name = "share_index")
    private List<ExpenseShare> shares = new ArrayList<>();

    // A hónap hányadik napján jön létre (1–28, hogy minden hónapban létezzen)
    @Column(nullable = false)
    private int dayOfMonth;

    // A következő létrehozás dátuma
    @Column(nullable = false)
    private LocalDate nextRunDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Currency getCurrency() {
        return currency == null ? Currency.HUF : currency;
    }

    public Category getCategory() {
        return category == null ? Category.EGYEB : category;
    }

    public SplitType getSplitType() {
        return splitType == null ? SplitType.EQUAL : splitType;
    }
}
