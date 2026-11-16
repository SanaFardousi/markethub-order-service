package com.markethub.orders.unit;

import com.markethub.orders.application.Carrier;
import com.markethub.orders.application.ShippingOption;
import com.markethub.orders.application.ShippingService;
import com.markethub.orders.config.ExecutorConfig;
import com.markethub.orders.domain.exception.NoCarrierAvailableException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Slf4j
class CarrierExecutorComparisonTest {

    private static final int REQUESTS = 100;               // 100 shipping requests at once...
    private static final int CARRIERS = 3;                 // ...each asking 3 carriers = 300 blocking calls
    private static final long CARRIER_MILLIS = 200;

    private record Outcome(int answers, int requestsWithNoAnswer, boolean sawVirtualThread, Duration elapsed) {}

    /** A carrier that only waits, like a real network call. A timed await on a latch nobody opens is a pause without Thread.sleep. */
    private static Carrier blocking(String name, AtomicBoolean sawVirtualThread) {
        return new Carrier() {
            @Override public String name() { return name; }

            @Override public ShippingOption quote(String country) {
                if (Thread.currentThread().isVirtual()) {
                    sawVirtualThread.set(true);
                }
                try {
                    new CountDownLatch(1).await(CARRIER_MILLIS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new ShippingOption(name, new BigDecimal("1.000"), 2);
            }
        };
    }

    /** Sends the whole burst at the same instant, using the same latch idea as the M7 race tests. */
    private Outcome run(ExecutorService carrierExecutor) throws Exception {
        AtomicBoolean sawVirtualThread = new AtomicBoolean();
        ShippingService service = new ShippingService(
            List.of(blocking("A", sawVirtualThread), blocking("B", sawVirtualThread), blocking("C", sawVirtualThread)),
            carrierExecutor);
        AtomicInteger answers = new AtomicInteger();
        AtomicInteger requestsWithNoAnswer = new AtomicInteger();
        ExecutorService requestThreads = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch ready = new CountDownLatch(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                futures.add(requestThreads.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        answers.addAndGet(service.options("KW").size());
                    } catch (NoCarrierAvailableException none) {
                        requestsWithNoAnswer.incrementAndGet();
                    }
                    return null;
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            long begin = System.nanoTime();
            start.countDown();
            for (Future<Object> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            return new Outcome(answers.get(), requestsWithNoAnswer.get(), sawVirtualThread.get(),
                Duration.ofNanos(System.nanoTime() - begin));
        } finally {
            requestThreads.shutdownNow();
        }
    }

    @Test
    @DisplayName("300 blocking carrier calls at once: the bounded platform pool drops most, virtual threads answer all")
    void virtualThreads_shouldAnswerEveryCarrierCall_whereTheBoundedPlatformPoolSkipsMost() throws Exception {
        ThreadPoolExecutor platform = new ExecutorConfig().carrierExecutor();
        ExecutorService virtual = new ExecutorConfig().virtualCarrierExecutor();
        try {
            Outcome onPlatform = run(platform);
            Outcome onVirtual = run(virtual);

            log.info("Platform pool ({} threads, queue {}): {} of {} calls answered, {} requests got no answer at all, {} ms",
                ExecutorConfig.THREADS, ExecutorConfig.QUEUE_SIZE, onPlatform.answers(), REQUESTS * CARRIERS,
                onPlatform.requestsWithNoAnswer(), onPlatform.elapsed().toMillis());
            log.info("Virtual threads: {} of {} calls answered, {} requests got no answer at all, {} ms",
                onVirtual.answers(), REQUESTS * CARRIERS, onVirtual.requestsWithNoAnswer(),
                onVirtual.elapsed().toMillis());

            assertAll(
                () -> assertThat(onVirtual.answers()).isEqualTo(REQUESTS * CARRIERS),
                () -> assertThat(onVirtual.requestsWithNoAnswer()).isZero(),
                () -> assertThat(onVirtual.sawVirtualThread()).isTrue(),
                () -> assertThat(onPlatform.answers()).isLessThan(REQUESTS * CARRIERS),
                () -> assertThat(onPlatform.sawVirtualThread()).isFalse());
        } finally {
            platform.shutdownNow();
            virtual.shutdownNow();
        }
    }
}
