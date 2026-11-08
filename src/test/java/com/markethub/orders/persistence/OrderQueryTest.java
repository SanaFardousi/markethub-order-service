package com.markethub.orders.persistence;

import com.markethub.orders.domain.model.*;
import com.markethub.orders.infrastructure.persistence.CustomerRepository;
import com.markethub.orders.infrastructure.persistence.OrderRepository;
import com.markethub.orders.infrastructure.persistence.OrderSummary;
import com.markethub.orders.infrastructure.persistence.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static com.markethub.orders.OrderFixtures.anOrder;
import static com.markethub.orders.persistence.PersistenceFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderQueryTest extends AbstractPersistenceTest {

    @Autowired
    OrderRepository orders;
    @Autowired
    CustomerRepository customers;
    @Autowired
    ProductRepository products;
    @Autowired
    EntityManager em;
    @Autowired
    EntityManagerFactory emf;

    private Statistics statistics() { return emf.unwrap(SessionFactory.class).getStatistics(); }

    private Customer savedCustomer(String email) {
        return customers.saveAndFlush(new Customer(email, CustomerTier.SILVER, "KW"));
    }

    @Test
    void findSummaries_shouldReturnNewestFirstWithoutLoadingOrderEntities() {
        Customer customer = savedCustomer("history@example.com");
        orders.saveAndFlush(anOrder().withCustomer(customer).withOrderNumber("MH-Q-1").withCreatedAt(NOW).build());
        orders.saveAndFlush(anOrder().withCustomer(customer).withOrderNumber("MH-Q-2")
            .withCreatedAt(NOW.plusSeconds(60)).build());
        em.clear();
        statistics().clear();

        Page<OrderSummary> page = orders.findSummaries(customer.getId(),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertAll(
            () -> assertThat(page.getContent()).extracting(OrderSummary::orderNumber)
                .containsExactly("MH-Q-2", "MH-Q-1"),
            () -> assertThat(statistics().getEntityLoadCount()).isZero());
    }

    @Test
    void findSummariesByStatus_shouldReturnOnlyOrdersWithThatStatus() {
        Customer customer = savedCustomer("filter@example.com");
        orders.saveAndFlush(anOrder().withCustomer(customer).withOrderNumber("MH-F-1")
            .withStatus(OrderStatus.CONFIRMED).build());
        orders.saveAndFlush(anOrder().withCustomer(customer).withOrderNumber("MH-F-2")
            .withStatus(OrderStatus.SHIPPED).build());

        Page<OrderSummary> shipped = orders.findSummariesByStatus(customer.getId(), OrderStatus.SHIPPED, PageRequest.of(0, 10));

        assertThat(shipped.getContent()).extracting(OrderSummary::orderNumber).containsExactly("MH-F-2");
    }

    @Test
    void countByCustomerId_shouldCountOnlyThatCustomersOrders() {
        Customer busy = savedCustomer("busy@example.com");
        Customer quiet = savedCustomer("quiet@example.com");
        orders.saveAndFlush(anOrder().withCustomer(busy).withOrderNumber("MH-C-1").build());
        orders.saveAndFlush(anOrder().withCustomer(busy).withOrderNumber("MH-C-2").build());

        assertAll(
            () -> assertThat(orders.countByCustomerId(busy.getId())).isEqualTo(2),
            () -> assertThat(orders.countByCustomerId(quiet.getId())).isZero());
    }

    @Test
    void findBySkuIn_shouldReturnOnlyKnownSkus() {
        List<Product> found = products.findBySkuIn(List.of("MH-OUD-50", "MH-ROSE-30", "NOT-A-SKU"));

        assertThat(found).extracting(Product::getSku).containsExactlyInAnyOrder("MH-OUD-50", "MH-ROSE-30");
    }

    @Test
    void findWithDetailsByIdempotencyKey_shouldLoadOrderWithLinesAndPayment() {
        Customer customer = savedCustomer("lookup@example.com");
        orders.saveAndFlush(anOrder().withCustomer(customer).withOrderNumber("MH-L-1")
            .withIdempotencyKey("lookup-key").withLineCount(3).build());
        em.clear();

        Order found = orders.findWithDetailsByIdempotencyKey("lookup-key").orElseThrow();

        assertAll(
            () -> assertThat(found.getOrderNumber()).isEqualTo("MH-L-1"),
            () -> assertThat(found.getLines()).hasSize(3),
            () -> assertThat(found.getPayment()).isNotNull());
    }
}
