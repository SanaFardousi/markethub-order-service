package com.markethub.orders.infrastructure.outbox;

public record EventEnvelope(String eventId, String type, String occurredAt,
                            String aggregateId, Object payload) {}
