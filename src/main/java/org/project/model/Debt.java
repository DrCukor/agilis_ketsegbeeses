package org.project.model;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "Debts")
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "Expenses_id", nullable = false)
    private Expense expense;

    @Column(nullable = false)
    private String debtor;

    @Column(nullable = false)
    private String creditor;

    @Column(nullable = false)
    private double amount;

    public Debt(Expense expense, String debtor, String creditor, double amount) {
        setExpense(expense);
        setDebtor(debtor);
        setCreditor(creditor);
        setAmount(amount);
    }

    protected Debt() {

    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        if(expense == null){
            throw new IllegalArgumentException("A kiadás nem lehet null!");
        }
        this.expense = expense;
    }

    public String getDebtor() {
        return debtor;
    }

    public void setDebtor(String debtor) {
        if(debtor == null || debtor.isBlank()){
            throw new IllegalArgumentException("A tartozó neve nem lehet üres!");
        }
        this.debtor = debtor;
    }

    public String getCreditor() {
        return creditor;
    }

    public void setCreditor(String creditor) {
        if(creditor == null || creditor.isBlank()){
            throw new IllegalArgumentException("A fizető neve nem lehet üres!");
        }
        this.creditor = creditor;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        if(!Double.isFinite(amount) || amount < 0){
            throw new IllegalArgumentException("Az összegnek pozitívnak kell lennie!");
        }
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Debt debt = (Debt) o;
        return Double.compare(amount, debt.amount) == 0 && Objects.equals(id, debt.id) && Objects.equals(expense, debt.expense) && Objects.equals(debtor, debt.debtor) && Objects.equals(creditor, debt.creditor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, expense, debtor, creditor, amount);
    }

    @Override
    public String toString() {
        return "Debt{" +
                "id=" + id +
                ", expense=" + expense +
                ", debtor='" + debtor + '\'' +
                ", creditor='" + creditor + '\'' +
                ", amount=" + amount +
                '}';
    }
}
