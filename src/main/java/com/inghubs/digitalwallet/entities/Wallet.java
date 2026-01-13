package com.inghubs.digitalwallet.entities;

import java.util.UUID;

import com.inghubs.digitalwallet.utilities.enums.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Customer customer;

    private String walletName;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private Boolean isActiveShopping;

    private Boolean isActiveWithdraw;

    @Builder.Default
    private Double balance = 0.0;

    @Builder.Default
    private Double usableBalance = 0.0;
}
