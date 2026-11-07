package com.markethub.orders.application;

import com.markethub.orders.domain.model.Product;
import com.markethub.orders.infrastructure.persistence.ProductRepository;
import com.markethub.orders.web.dto.ProductResponse;
import com.markethub.orders.web.dto.ProductUpsertRequest;
import com.markethub.orders.web.mapper.ApiMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository products;
    private final ViewMapper viewMapper;
    private final Clock clock;

    @Transactional
    public ProductView upsert(ProductCommand c) {
        Instant now = clock.instant();
        Product product = products.findBySku(c.sku()).orElse(null);
        if (product == null) {
            product = new Product(c.sku(), c.nameEn(), c.nameAr(), c.brand(), c.category(), c.price(), c.status(), now);
        } else {
            product.update(c.nameEn(), c.nameAr(), c.brand(), c.category(), c.price(), c.status(), now);
        }
        return viewMapper.toView(products.saveAndFlush(product));   // flush so the response shows the new @Version
    }
}

