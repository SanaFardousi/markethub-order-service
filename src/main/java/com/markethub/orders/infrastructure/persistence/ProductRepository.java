package com.markethub.orders.infrastructure.persistence;

import com.markethub.orders.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}
