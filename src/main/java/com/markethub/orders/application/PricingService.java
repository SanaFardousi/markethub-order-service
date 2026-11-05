package com.markethub.orders.application;

import com.markethub.orders.domain.exception.ConflictException;
import com.markethub.orders.domain.exception.NotFoundException;
import com.markethub.orders.domain.model.Customer;
import com.markethub.orders.domain.model.Product;
import com.markethub.orders.domain.model.ProductStatus;
import com.markethub.orders.domain.payment.PaymentMethod;
import com.markethub.orders.domain.pricing.Basket;
import com.markethub.orders.domain.pricing.BasketLine;
import com.markethub.orders.domain.pricing.CustomerFact;
import com.markethub.orders.domain.pricing.PricingResult;
import com.markethub.orders.infrastructure.drools.DroolsPricingEngine;
import com.markethub.orders.infrastructure.persistence.CustomerRepository;
import com.markethub.orders.infrastructure.persistence.OrderRepository;
import com.markethub.orders.infrastructure.persistence.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingService {
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final DroolsPricingEngine engine;

    @Transactional(readOnly = true)
    public PricingResult price(Long customerId, String couponCode, PaymentMethod paymentMethod, List<RequestedLine> requestedLines){
        Customer customer = customers.findById(customerId)
            .orElseThrow(() -> new NotFoundException("Customer " + customers + " not found"));

        Set<String> skus = requestedLines.stream().map(RequestedLine::sku).collect(Collectors.toSet());
        Map<String, Product> productsBySku = products.findBySkuIn(skus).stream()
            .collect(Collectors.toMap(Product::getSku, Function.identity()));

        List<BasketLine> lines = new ArrayList<>();
        for (RequestedLine requested : requestedLines) {
            Product product = productsBySku.get(requested.sku());
            if (product == null || product.getStatus() != ProductStatus.ACTIVE) {
                throw new ConflictException("PRODUCT_NOT_AVAILABLE", "Unknown or inactive SKU: " + requested.sku());
            }
            lines.add(new BasketLine(product.getSku(), requested.quantity(), product.getPriceAmount()));
        }

        Basket basket = new Basket(lines, couponCode, paymentMethod);
        CustomerFact customerFact = new CustomerFact(customer.getTier(), orders.countByCustomerId(customerId));
        return engine.price(basket, customerFact);
    }

}
