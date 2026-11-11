package com.markethub.orders.infrastructure.provider.carriers;

import com.markethub.orders.application.Carrier;
import com.markethub.orders.application.CarrierUnavailableException;
import com.markethub.orders.application.ShippingOption;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

abstract class FakeCarrier implements Carrier {

    private final String name;
    private final BigDecimal domesticPrice;
    private final int domesticDays;
    private final Duration delay;
    private final double failureRate;

    FakeCarrier(String name, String domesticPrice, int domesticDays, Duration delay, double failureRate) {
        this.name = name;
        this.domesticPrice = new BigDecimal(domesticPrice);
        this.domesticDays = domesticDays;
        this.delay = delay;
        this.failureRate = failureRate;
    }

    @Override
    public String name() { return name; }

    @Override
    public ShippingOption quote(String country) {
        pause();
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {     // 0.0 never fails, 1.0 always fails
            throw new CarrierUnavailableException(name + " is unavailable");
        }
        boolean domestic = "KW".equalsIgnoreCase(country);
        BigDecimal price = domestic ? domesticPrice : domesticPrice.add(new BigDecimal("4.000"));
        return new ShippingOption(name, price, domestic ? domesticDays : domesticDays + 3);
    }

    /** Pretends the network call takes a while. */
    private void pause() {
        try {
            TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();                            // keep the "stop!" signal alive
            throw new CarrierUnavailableException(name + " was interrupted");
        }
    }
}
