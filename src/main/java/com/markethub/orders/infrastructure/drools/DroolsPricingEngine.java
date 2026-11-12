package com.markethub.orders.infrastructure.drools;

import java.util.Collection;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Component;

import com.markethub.orders.domain.pricing.Basket;
import com.markethub.orders.domain.pricing.Candidate;
import com.markethub.orders.domain.pricing.CustomerFact;
import com.markethub.orders.domain.pricing.Discount;
import com.markethub.orders.domain.pricing.FraudHold;
import com.markethub.orders.domain.pricing.PricingResult;
import com.markethub.orders.domain.pricing.Shipping;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class DroolsPricingEngine {

    private final KieContainer kieContainer;
    private final Timer pricingTimer;

    public DroolsPricingEngine(KieContainer kieContainer, MeterRegistry registry) {
        this.kieContainer = kieContainer;
        this.pricingTimer = Timer.builder("markethub.pricing.duration")
            .description("Time spent running the pricing rules")
            .register(registry);
    }

    public PricingResult price(Basket basket, CustomerFact customer) {
        return pricingTimer.record(() -> run(basket, customer));
    }

    private PricingResult run(Basket basket, CustomerFact customer) {
        KieSession session = kieContainer.newKieSession();   // NOT thread-safe: one per call
        try {
            session.insert(basket);
            session.insert(customer);
            session.fireAllRules();
            Collection<?> facts = session.getObjects(o -> o instanceof Candidate || o instanceof Discount
                || o instanceof Shipping || o instanceof FraudHold);
            return PricingResult.from(basket, facts);
        } finally {
            session.dispose();
        }
    }
}
