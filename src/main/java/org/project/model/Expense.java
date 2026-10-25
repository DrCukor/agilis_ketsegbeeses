package org.project.model;

import javax.persistence.*;
import javax.persistence.criteria.Fetch;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "Expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private double amount;
    private String payer;



    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Expenses_participants", joinColumns = @JoinColumn(name = "Expenses_id"))
    @Column(name = "Person")
    private List<String> participants;

    public Expense(String title, double amount, String payer,List<String> participants) {
        setTitle(title);
        setAmount(amount);
        setPayer(payer);
        setParticipants(participants);
    }

    protected Expense(){

    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if(title.isBlank() || title == null){
            throw new IllegalArgumentException("A kiadás címe nem lehet üres!");
        }
        this.title = title;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        if(payer.isBlank() || payer == null){
            throw new IllegalArgumentException("A fizető neve nem lehet üres!");
        }
        this.payer = payer;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        if(!Double.isFinite(amount) || amount < 0){
            throw new IllegalArgumentException("Az összegnek pozitív számnak kell lennie!");
        }
        this.amount = amount;
    }
    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        if(participants == null
                || (participants.size() != 2
                && participants.size() != 4)
                || participants.stream().anyMatch(p -> p == null || p.isBlank())
                || participants.stream().distinct().count() != participants.size()
        ){
            throw new IllegalArgumentException("Pontosan 2 vagy pontosan 4 különböző résztvevőnek kell lennie!");
        }
        this.participants = new ArrayList<>(participants);
    }
    public double getSharePerPerson(){
        return amount/participants.size();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return Double.compare(amount, expense.amount) == 0 && Objects.equals(id, expense.id) && Objects.equals(title, expense.title) && Objects.equals(payer, expense.payer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, amount, payer);
    }



    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", amount=" + amount +
                ", payer='" + payer + '\'' +
                '}';
    }
}
