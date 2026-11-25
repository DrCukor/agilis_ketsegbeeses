package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Csoport: saját kiadásokkal, tartozásokkal és tagokkal (pl. lakótársak, egy nyaralás társasága)
@Entity
@Table(name = "expense_groups")
@Getter
@Setter
@NoArgsConstructor
public class ExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    // Meghívókód a csatlakozáshoz, pl. "K7F3-QM2P"
    @Column(nullable = false, unique = true, length = 20)
    private String inviteCode;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ExpenseGroup(String name, String description, String inviteCode, String createdBy) {
        this.name = name;
        this.description = description;
        this.inviteCode = inviteCode;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }
}
