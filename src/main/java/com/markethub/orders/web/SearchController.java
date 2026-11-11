package com.markethub.orders.web;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.markethub.orders.application.SearchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

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
