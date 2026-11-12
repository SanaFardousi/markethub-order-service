package com.markethub.orders.web;

import com.markethub.orders.application.ProductService;
import com.markethub.orders.web.dto.ProductResponse;
import com.markethub.orders.web.dto.ProductUpsertRequest;
import com.markethub.orders.web.mapper.ApiMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Products (admin)", description = "Create or update a product and publish ProductUpserted")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed request",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected error (a bug)",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
})

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {
    private final ProductService productService;
    private final ApiMapper mapper;

    @Operation(summary = "Create or update a product",
        description = "Saves the product and publishes a ProductUpserted event, which updates the search index.")
    @ApiResponse(responseCode = "200", description = "The saved product, including its new version")
    @PutMapping("/{sku}")
    public ProductResponse upsert(@PathVariable String sku, @Valid @RequestBody ProductUpsertRequest request) {
        return mapper.toResponse(productService.upsert(mapper.toCommand(sku, request)));
    }
}
