package com.markethub.orders.infrastructure.provider.carriers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GulfPost extends FakeCarrier {
    public GulfPost(@Value("${markethub.carriers.gulf-post.delay:300ms}") Duration delay,
                    @Value("${markethub.carriers.gulf-post.failure-rate:0.0}") double failureRate) {
        super("GulfPost", "1.500", 4, delay, failureRate);
    }
}
