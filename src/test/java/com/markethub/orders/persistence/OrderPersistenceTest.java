package com.markethub.orders.persistence;


import com.markethub.orders.domain.model.Customer;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.payment.CardPayment;
import com.markethub.orders.infrastructure.persistence.CustomerRepository;
import com.markethub.orders.infrastructure.persistence.OrderRepository;
import com.markethub.orders.infrastructure.persistence.OrderSummary;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.markethub.orders.persistence.PersistenceFixtures.customer;
import static com.markethub.orders.persistence.PersistenceFixtures.order;
import static org.assertj.core.api.Assertions.assertThat;


class OrderPersistenceTest extends AbstractPersistenceTest {
    @Autowired
    OrderRepository orders;
    @Autowired
    CustomerRepository customers;
    @Autowired
    EntityManager em;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    JdbcTemplate jdbc;

    private Statistics statistics() { return emf.unwrap(SessionFactory.class).getStatistics(); }

    @Test
    void removeLine_shouldDeleteRow_whenLineIsRemovedFromOrder() {
        Customer c = customers.saveAndFlush(customer("orphan@example.com"));
        Long id = orders.saveAndFlush(order(c, "MH-O-1", "key-o1", 3)).getId();
        em.clear();

        Order loaded = orders.findWithDetailsByOrderNumber("MH-O-1").orElseThrow();
        loaded.getLines().remove(0);
        em.flush();

        Long rows = jdbc.queryForObject("select count(*) from order_line where order_id = ?", Long.class, id);
        assertThat(rows).isEqualTo(2);
    }

    @Test
    void findWithDetails_shouldIssueOneStatement_whenOrderHasTenLines() {
        Customer c = customers.saveAndFlush(customer("stats@example.com"));
        orders.saveAndFlush(order(c, "MH-Q-1", "key-q1", 10));
        em.clear();
        statistics().clear();

        Order loaded = orders.findWithDetailsByOrderNumber("MH-Q-1").orElseThrow();

        assertThat(loaded.getLines()).hasSize(10);
        assertThat(loaded.getPayment()).isInstanceOf(CardPayment.class);
        assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
    }

}
