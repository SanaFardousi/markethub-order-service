package com.markethub.orders.unit;

import com.markethub.orders.config.ExecutorConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

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
}
