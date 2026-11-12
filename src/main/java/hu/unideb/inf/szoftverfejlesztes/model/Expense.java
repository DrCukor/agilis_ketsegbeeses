package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(nullable = false)
    private String payer;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_participants", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "person")
    private List<String> participants = new ArrayList<>();

    public Expense(String title, double amount, String payer, List<String> participants) {
        this.title = title;
        this.amount = amount;
        this.payer = payer;
        this.participants = participants;
    }

    public double getSharePerPerson() {
        if (participants == null || participants.isEmpty()) {
            return 0.0;
        }
        return amount / participants.size();
    }
}
