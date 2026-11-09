package com.markethub.orders.infrastructure.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    private String id;                       // the event id (a UUID), created by our code, not by the database
    private String aggregateId;              // the order number or SKU
    private String eventType;
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;                  // the whole JSON envelope that will be sent to Kafka

    private Instant createdAt;
    private Instant publishedAt;             // null until Kafka has acknowledged the message

    public OutboxEvent(String id, String aggregateId, String eventType, String topic,
                       String payload, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant now) { this.publishedAt = now; }
}
