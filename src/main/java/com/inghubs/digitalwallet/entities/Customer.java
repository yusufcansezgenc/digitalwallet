package com.inghubs.digitalwallet.entities;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String surname;

    private String TCKN;

    @OneToOne(mappedBy = "customer")
    private User user;
}
