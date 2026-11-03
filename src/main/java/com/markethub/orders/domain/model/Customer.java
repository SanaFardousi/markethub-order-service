package com.markethub.orders.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    @Enumerated(EnumType.STRING)
    private CustomerTier tier;
    private String country;

    public Customer(String email, CustomerTier tier, String country) {
        this.email = email;
        this.tier = tier;
        this.country = country;
    }
}
