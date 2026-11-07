package com.markethub.orders.infrastructure.persistence;

import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        select new com.markethub.orders.infrastructure.persistence.OrderSummary(
            o.orderNumber, o.status, o.total, o.createdAt)
        from Order o where o.customer.id = :customerId""")
    Page<OrderSummary> findSummaries(@Param("customerId") Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"lines", "payment"})
    Optional<Order> findWithDetailsByOrderNumber(String orderNumber);

    long countByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"lines", "payment"})
    Optional<Order> findWithDetailsByIdempotencyKey(String idempotencyKey);

    @Query("""
    select new com.markethub.orders.infrastructure.persistence.OrderSummary(
        o.orderNumber, o.status, o.total, o.createdAt)
    from Order o where o.customer.id = :customerId and o.status = :status""")
    Page<OrderSummary> findSummariesByStatus(@Param("customerId") Long customerId,
                                             @Param("status") OrderStatus status, Pageable pageable);


}
