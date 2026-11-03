package com.markethub.orders.persistence;

import com.markethub.orders.domain.model.Customer;
import com.markethub.orders.infrastructure.persistence.CustomerRepository;
import com.markethub.orders.infrastructure.persistence.OrderRepository;
import com.markethub.orders.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static com.markethub.orders.persistence.PersistenceFixtures.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class UniqueConstraintTest extends AbstractPersistenceTest {
    @Autowired
    ProductRepository products;
    @Autowired
    CustomerRepository customers;
    @Autowired
    OrderRepository orders;

    @Test
    void saveProduct_shouldReject_whenSkuIsDuplicated() {
        products.saveAndFlush(product("DUP-1"));
        assertThatThrownBy(() -> products.saveAndFlush(product("DUP-1")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveOrder_shouldReject_whenOrderNumberIsDuplicated() {
        Customer c = customers.saveAndFlush(customer("c1@example.com"));
        orders.saveAndFlush(order(c, "MH-D-1", "key-a", 1));
        assertThatThrownBy(() -> orders.saveAndFlush(order(c, "MH-D-1", "key-b", 1)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveOrder_shouldReject_whenIdempotencyKeyIsDuplicated() {
        Customer c = customers.saveAndFlush(customer("c2@example.com"));
        orders.saveAndFlush(order(c, "MH-D-2", "same-key", 1));
        assertThatThrownBy(() -> orders.saveAndFlush(order(c, "MH-D-3", "same-key", 1)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
