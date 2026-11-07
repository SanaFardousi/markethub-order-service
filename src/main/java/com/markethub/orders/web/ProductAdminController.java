package com.markethub.orders.web;

import com.markethub.orders.application.ProductService;
import com.markethub.orders.web.dto.ProductResponse;
import com.markethub.orders.web.dto.ProductUpsertRequest;
import com.markethub.orders.web.mapper.ApiMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {
    private final ProductService productService;
    private final ApiMapper mapper;

    @PutMapping("/{sku}")
    public ProductResponse upsert(@PathVariable String sku, @Valid @RequestBody ProductUpsertRequest request) {
        return mapper.toResponse(productService.upsert(mapper.toCommand(sku, request)));
    }
}
