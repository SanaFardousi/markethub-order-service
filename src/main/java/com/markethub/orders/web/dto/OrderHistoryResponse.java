package com.markethub.orders.web.dto;

import java.util.List;

public record OrderHistoryResponse(List<OrderSummaryResponse> content, int page, int size,
                                   long totalElements, int totalPages) {}
