package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Egy résztvevő része egy kiadásból
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseShare {

    @Column(name = "person", nullable = false)
    private String person;

    // A megadott érték a megosztási mód szerint: százalék, összeg vagy részarány (egyenlőnél üres).
    // Szerkesztéskor ebből töltjük vissza a csúszkákat.
    @Column(name = "input_value")
    private Double inputValue;

    // A ténylegesen rá eső, kerekített összeg a kiadás pénznemében
    @Column(name = "amount", nullable = false)
    private double amount;
}
