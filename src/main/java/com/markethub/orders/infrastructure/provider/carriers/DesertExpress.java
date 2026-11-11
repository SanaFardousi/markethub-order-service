package com.markethub.orders.infrastructure.provider.carriers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DesertExpress extends FakeCarrier {
    public DesertExpress(@Value("${markethub.carriers.desert-express.delay:500ms}") Duration delay,
                         @Value("${markethub.carriers.desert-express.failure-rate:0.0}") double failureRate) {
        super("DesertExpress", "2.500", 2, delay, failureRate);
    }
}
