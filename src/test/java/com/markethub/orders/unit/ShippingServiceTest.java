package com.markethub.orders.unit;

import com.markethub.orders.application.Carrier;
import com.markethub.orders.application.CarrierUnavailableException;
import com.markethub.orders.application.ShippingOption;
import com.markethub.orders.application.ShippingService;
import com.markethub.orders.domain.exception.NoCarrierAvailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class ShippingServiceTest {

    private record TestCarrier(String name, Supplier<ShippingOption> behaviour) implements Carrier {
        @Override public ShippingOption quote(String country) { return behaviour.get(); }
    }

    private ThreadPoolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ThreadPoolExecutor(6, 6, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
    }

    @AfterEach
    void tearDown() { executor.shutdownNow(); }      // also interrupts any carrier still "waiting"

    private ShippingService service(Carrier... carriers) { return new ShippingService(List.of(carriers), executor); }

    private static ShippingOption option(String name, String price) {
        return new ShippingOption(name, new BigDecimal(price), 2);
    }

    private static Carrier answering(String name, String price) {
        return new TestCarrier(name, () -> option(name, price));
    }

    private static Carrier failing(String name) {
        return new TestCarrier(name, () -> { throw new CarrierUnavailableException(name + " is down"); });
    }

    /** Waits for the given time on a latch nobody opens: a pause that needs no Thread.sleep. */
    private static Carrier takingMillis(String name, long millis) {
        return new TestCarrier(name, () -> {
            try {
                new CountDownLatch(1).await(millis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return option(name, "1.000");
        });
    }

    @Test
    void options_shouldReturnAnswersCheapestFirst() {
        List<ShippingOption> options = service(answering("A", "3.500"), answering("B", "1.500"),
            answering("C", "2.500")).options("KW");

        assertThat(options).extracting(ShippingOption::carrier).containsExactly("B", "C", "A");
    }

    @Test
    @DisplayName("a carrier that takes 3 seconds does not hold back the answer")
    void options_shouldAnswerInUnderOneSecond_whenOneCarrierTakesThreeSeconds() {
        ShippingService service = service(answering("Fast", "3.500"), takingMillis("Slow", 3000),
            answering("Gulf", "1.500"));

        long start = System.nanoTime();
        List<ShippingOption> options = service.options("KW");
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertAll(
            () -> assertThat(elapsed).isLessThan(Duration.ofSeconds(1)),
            () -> assertThat(options).extracting(ShippingOption::carrier).containsExactly("Gulf", "Fast"));
    }

    @Test
    void options_shouldIgnoreCarriersThatFail() {
        List<ShippingOption> options = service(failing("Broken"), answering("Works", "2.000")).options("KW");

        assertThat(options).extracting(ShippingOption::carrier).containsExactly("Works");
    }

    @Test
    void options_shouldThrowNoCarrierAvailable_whenEveryCarrierFails() {
        assertThatThrownBy(() -> service(failing("A"), failing("B")).options("KW"))
            .isInstanceOf(NoCarrierAvailableException.class);
    }

    @Test
    void options_shouldThrowNoCarrierAvailable_whenEveryCarrierIsTooSlow() {
        assertThatThrownBy(() -> service(takingMillis("A", 3000), takingMillis("B", 3000)).options("KW"))
            .isInstanceOf(NoCarrierAvailableException.class);
    }

    @Test
    @DisplayName("a full pool skips carriers instead of failing with a server error")
    void options_shouldSkipCarriersWithoutAFreeThread() {
        ThreadPoolExecutor tiny = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
        try {
            ShippingService service = new ShippingService(
                List.of(takingMillis("First", 200), answering("Second", "1.000"), answering("Third", "2.000")), tiny);

            assertThat(service.options("KW")).extracting(ShippingOption::carrier).containsExactly("First");
        } finally {
            tiny.shutdownNow();
        }
    }
}
