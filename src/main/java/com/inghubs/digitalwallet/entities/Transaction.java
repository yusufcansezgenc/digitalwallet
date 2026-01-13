package com.inghubs.digitalwallet.entities;

import java.util.UUID;

import com.inghubs.digitalwallet.utilities.enums.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Wallet wallet;

    @Positive
    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private OppositePartyType oppositePartyType;

    private String oppositeParty;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
}
