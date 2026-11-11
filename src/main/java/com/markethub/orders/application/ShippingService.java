package com.markethub.orders.application;

import com.markethub.orders.domain.exception.NoCarrierAvailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShippingService {

    private static final long TIMEOUT_MILLIS = 800;

    private final List<Carrier> carriers;
    private final ThreadPoolExecutor carrierExecutor;

    public ShippingService(List<Carrier> carriers, ThreadPoolExecutor carrierExecutor) {
        this.carriers = carriers;
        this.carrierExecutor = carrierExecutor;
    }

    public List<ShippingOption> options(String country) {
        List<CompletableFuture<Optional<ShippingOption>>> calls = carriers.stream()
            .map(carrier -> ask(carrier, country))           // start all calls first...
            .toList();

        List<ShippingOption> answered = calls.stream()
            .map(CompletableFuture::join)                    // ...then collect the answers
            .flatMap(Optional::stream)
            .sorted(Comparator.comparing(ShippingOption::price))
            .toList();

        if (answered.isEmpty()) {
            throw new NoCarrierAvailableException();             // becomes a 503
        }
        return answered;
    }

    private CompletableFuture<Optional<ShippingOption>> ask(Carrier carrier, String country) {
        try {
            return CompletableFuture.supplyAsync(() -> quote(carrier, country), carrierExecutor)
                .orTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .thenApply(Optional::of)
                .exceptionally(failure -> {
                    log.warn("{} gave no answer: {}", carrier.name(), failure.toString());
                    return Optional.empty();
                });
        } catch (RejectedExecutionException busy) {
            log.warn("{} skipped: every carrier thread is busy", carrier.name());
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private ShippingOption quote(Carrier carrier, String country) {
        log.info("Asking {} for a quote", carrier.name());       // runs on a carrier-N thread, so the log shows its name
        return carrier.quote(country);
    }
}
