package com.markethub.orders.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorConfig {

    public static final int THREADS = 12;
    public static final int QUEUE_SIZE = 100;

    @Bean(destroyMethod = "")
    @ConditionalOnProperty(name = "markethub.carriers.executor", havingValue = "platform", matchIfMissing = true)
    public ThreadPoolExecutor carrierExecutor() {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory names = task -> {
            Thread thread = new Thread(task, "carrier-" + counter.incrementAndGet());   // carrier-1, carrier-2, ...
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(THREADS, THREADS, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(QUEUE_SIZE), names, new ThreadPoolExecutor.AbortPolicy());
    }

    /** The alternative: one new virtual thread per task. Blocking is cheap, and there is no pool limit. */
    @Bean(name = "carrierExecutor", destroyMethod = "")
    @ConditionalOnProperty(name = "markethub.carriers.executor", havingValue = "virtual")
    public ExecutorService virtualCarrierExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("carrier-vt-", 1).factory());
    }

    @Bean
    public DisposableBean carrierExecutorShutdown(@Qualifier("carrierExecutor") ExecutorService carrierExecutor) {
        return () -> {
            carrierExecutor.shutdown();                                       // stop accepting new work
            if (!carrierExecutor.awaitTermination(5, TimeUnit.SECONDS)) {     // let running quotes finish
                carrierExecutor.shutdownNow();                                // then interrupt what is left
            }
        };
    }
}
