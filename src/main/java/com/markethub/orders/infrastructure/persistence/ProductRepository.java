package com.markethub.orders.infrastructure.persistence;

import com.markethub.orders.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySkuIn(Collection<String> skus);
    Optional<Product> findBySku(String sku);
}
