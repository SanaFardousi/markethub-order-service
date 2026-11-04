package com.markethub.orders.domain.pricing;

import com.markethub.orders.domain.model.CustomerTier;
import lombok.Getter;

@Getter
public class CustomerFact {
    private final CustomerTier tier;
    private final long earlierOrders;

    public CustomerFact(CustomerTier tier, long earlierOrders) {
        this.tier = tier;
        this.earlierOrders = earlierOrders;
    }
}
