package com.markethub.orders.web;

import java.time.Instant;
import java.util.List;

public record ApiError(String code, String message, String traceId, Instant timestamp,
                       List<FieldIssue> fieldErrors) {
    public record FieldIssue(String field, String message) {}
}
