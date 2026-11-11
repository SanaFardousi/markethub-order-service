package com.markethub.orders.domain.model;

import java.util.Arrays;
import com.markethub.orders.domain.exception.BadRequestException;


public enum SearchSort {
    RELEVANCE("relevance"), PRICE_ASC("price_asc"), PRICE_DESC("price_desc");

    private final String param;
    SearchSort(String param) { this.param = param; }

    public static SearchSort fromParam(String value) {
        if (value == null || value.isBlank()) {
            return RELEVANCE;
        }
        return Arrays.stream(values()).filter(s -> s.param.equals(value)).findFirst()
            .orElseThrow(() -> new BadRequestException("Unknown sort: " + value));
    }
}

