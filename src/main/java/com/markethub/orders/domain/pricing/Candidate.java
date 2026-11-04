package com.markethub.orders.domain.pricing;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class Candidate {          // a possible discount (R1 or R2)
    private final String id;
    private final String description;
    private final BigDecimal amount;
    public Candidate(String id, String description, BigDecimal amount) {
        this.id = id; this.description = description; this.amount = amount;
    }
}

