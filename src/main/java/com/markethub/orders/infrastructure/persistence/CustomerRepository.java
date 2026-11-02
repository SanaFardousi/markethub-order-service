package com.markethub.orders.infrastructure.persistence;

import com.markethub.orders.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {}
