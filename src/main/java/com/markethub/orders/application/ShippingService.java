package com.markethub.orders.application;

import com.markethub.orders.domain.exception.NoCarrierAvailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class ShippingService {

    private static final long TIMEOUT_MILLIS = 800;

    private final List<Carrier> carriers;
    private final ExecutorService carrierExecutor;

    public ShippingService(List<Carrier> carriers, @Qualifier("carrierExecutor") ExecutorService carrierExecutor) {
        this.carriers = carriers;
        this.carrierExecutor = carrierExecutor;
    }

    /** Asks every carrier at the same time and keeps the answers that arrive within one shared deadline. */
    public List<ShippingOption> options(String country) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);

        Map<Carrier, Future<ShippingOption>> calls = new LinkedHashMap<>();
        for (Carrier carrier : carriers) {
            submit(carrier, country).ifPresent(call -> calls.put(carrier, call));
        }

        List<ShippingOption> answered = new ArrayList<>();
        calls.forEach((carrier, call) -> awaitAnswer(carrier, call, deadline).ifPresent(answered::add));
        answered.sort(Comparator.comparing(ShippingOption::price));

        if (answered.isEmpty()) {
            throw new NoCarrierAvailableException();
        }
        return answered;
    }

    private Optional<Future<ShippingOption>> submit(Carrier carrier, String country) {
        try {
            return Optional.of(carrierExecutor.submit(() -> quote(carrier, country)));
        } catch (RejectedExecutionException busy) {
            log.warn("{} skipped: every carrier thread is busy", carrier.name());
            return Optional.empty();
        }
    }

    /**
     * Waits for one carrier until the shared deadline. A carrier that is too late is cancelled with an
     * interrupt, so its thread stops waiting on the slow call and goes back to the pool at once.
     */
    private Optional<ShippingOption> awaitAnswer(Carrier carrier, Future<ShippingOption> call, long deadline) {
        try {
            long remaining = Math.max(0, deadline - System.nanoTime());
            return Optional.of(call.get(remaining, TimeUnit.NANOSECONDS));
        } catch (TimeoutException late) {
            call.cancel(true);
            log.warn("{} gave no answer within {} ms", carrier.name(), TIMEOUT_MILLIS);
        } catch (ExecutionException failed) {
            log.warn("{} gave no answer: {}", carrier.name(), failed.getCause().toString());
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();         // keep the "stop!" signal for our caller
        }
        return Optional.empty();
    }

    private ShippingOption quote(Carrier carrier, String country) {
        log.info("Asking {} for a quote", carrier.name());
        return carrier.quote(country);
    }
}
