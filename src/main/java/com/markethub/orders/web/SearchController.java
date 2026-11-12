package com.markethub.orders.web;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.markethub.orders.application.SearchService;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Search", description = "Full-text product search with filters, facets and paging")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed request",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected error (a bug)",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
})

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "Search products",
        description = "Full-text search over English and Arabic names. All filters are optional and combine with AND. "
            + "Sort is relevance, price_asc or price_desc. Page size is capped at 100.")
    @ApiResponse(responseCode = "200", description = "A page of hits with brand and category facets")
    @GetMapping("/products")
    public SearchResponse search(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) List<String> brand,
                                 @RequestParam(required = false) List<String> category,
                                 @RequestParam(required = false) BigDecimal minPrice,
                                 @RequestParam(required = false) BigDecimal maxPrice,
                                 @RequestParam(required = false) String sort,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer size) {
        return SearchResponse.from(searchService.search(q, brand, category, minPrice, maxPrice, sort, page, size));
    }
}
