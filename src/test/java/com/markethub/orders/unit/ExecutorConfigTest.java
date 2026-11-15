package com.markethub.orders.unit;

import com.markethub.orders.config.ExecutorConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import java.util.concurrent.ExecutorService;

class ExecutorConfigTest {

    private final ExecutorConfig config = new ExecutorConfig();

    @Test
    void carrierExecutor_shouldNameItsThreadsWithTheCarrierPrefix() throws Exception {
        ThreadPoolExecutor executor = config.carrierExecutor();
        try {
            String name = executor.submit(() -> Thread.currentThread().getName()).get(5, TimeUnit.SECONDS);

            assertThat(name).startsWith("carrier-");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void carrierExecutor_shouldBeBounded() {
        ThreadPoolExecutor executor = config.carrierExecutor();
        try {
            assertAll(
                () -> assertThat(executor.getMaximumPoolSize()).isEqualTo(ExecutorConfig.THREADS),
                () -> assertThat(executor.getQueue().remainingCapacity()).isEqualTo(ExecutorConfig.QUEUE_SIZE));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shutdownBean_shouldStopTheExecutor_andRefuseNewWork() throws Exception {
        ThreadPoolExecutor executor = config.carrierExecutor();
        executor.submit(() -> "warm up").get(5, TimeUnit.SECONDS);

        config.carrierExecutorShutdown(executor).destroy();

        assertAll(
            () -> assertThat(executor.isTerminated()).isTrue(),
            () -> assertThatThrownBy(() -> executor.submit(() -> "late"))
                .isInstanceOf(RejectedExecutionException.class));
    }

    @Test
    void virtualCarrierExecutor_shouldRunTasksOnNamedVirtualThreads() throws Exception {
        ExecutorService executor = config.virtualCarrierExecutor();
        try {
            record Seen(boolean virtual, String name) {}
            Seen seen = executor.submit(() -> new Seen(Thread.currentThread().isVirtual(), Thread.currentThread().getName()))
                .get(5, TimeUnit.SECONDS);

            assertAll(
                () -> assertThat(seen.virtual()).isTrue(),
                () -> assertThat(seen.name()).startsWith("carrier-vt-"));
        } finally {
            executor.shutdownNow();
        }
    }
}
