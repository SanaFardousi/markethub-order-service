package com.markethub.orders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorConfig {

    public static final int THREADS = 12;
    public static final int QUEUE_SIZE = 100;

    @Bean
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
}
