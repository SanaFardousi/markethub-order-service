package com.markethub.orders.infrastructure.provider.carriers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class FastCourier extends FakeCarrier {
    public FastCourier(@Value("${markethub.carriers.fast-courier.delay:100ms}") Duration delay,
                       @Value("${markethub.carriers.fast-courier.failure-rate:0.0}") double failureRate) {
        super("FastCourier", "3.500", 1, delay, failureRate);
    }
}

