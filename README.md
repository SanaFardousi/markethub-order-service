# MarketHub Order & Search Service

[![CI](https://github.com/SanaFardousi/markethub-order-service/actions/workflows/ci.yml/badge.svg)](https://github.com/SanaFarodusi/markethub-order-service/actions/workflows/ci.yml)

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.2-green?style=flat-square&logo=springboot&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-3.9.2-black?style=flat-square&logo=apachekafka&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-9.0.3-yellow?style=flat-square&logo=elasticsearch&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue?style=flat-square&logo=mysql&logoColor=white)
![Drools](https://img.shields.io/badge/Drools-10.2.0-red?style=flat-square&logo=redhat&logoColor=white)

A Spring Boot service for a Gulf e-commerce store. It prices baskets with Drools rules, takes orders and moves them through a lifecycle built on design patterns, publishes every order and product change to Kafka through a transactional outbox, serves product search from Elasticsearch, and stays correct under concurrent requests. Money is in Kuwaiti dinars with three decimals.

## Quick start

You need Docker (at least 6 GB of memory) and nothing else.

```powershell
git clone https://github.com/SanaFardousi/markethub-order-service.git
cd markethub-order-service
docker compose up -d --build
```
The first start builds the image and takes a few minutes. When `docker compose ps` shows the `app` service as `healthy`:

| What | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Prometheus metrics | http://localhost:8080/actuator/prometheus |
| Kafka UI | http://localhost:8090 |

The compose file uses throwaway passwords for local use. For anything else, copy `.env.example` to `.env` and set real values.

## Try it

**Price a basket**
```powershell
$body = '{"customerId":1,"couponCode":"WELCOME10","paymentMethod":"CARD","lines":[{"sku":"MH-OUD-50","quantity":2},{"sku":"MH-ROSE-30","quantity":1}]}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/pricing/preview -ContentType "application/json" -Body $body
```

Output:
```powershell
currency   : KWD
subtotal   : 33.000
discount   : 3.300
shipping   : 0.000
total      : 29.700
promotions : {@{id=R2; description=WELCOME10 coupon; amount=3.300}}
onHold     : False
```

**Place an order**
```powershell
$body = '{"customerId":1,"couponCode":"WELCOME10","paymentMethod":"CARD","card":{"number":"4111111111111111","expiry":"12/28"},"shippingAddress":{"country":"KW","city":"Kuwait City","line1":"Block 3, Street 12"},"lines":[{"sku":"MH-OUD-50","quantity":2},{"sku":"MH-ROSE-30","quantity":1}]}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/orders -ContentType "application/json" -Headers @{"Idempotency-Key"="3f6c0e2a-8d4b-4c1e-9a77-1b2c3d4e5f60"} -Body $body
```

Output:
```powershell
orderNumber : MH-2026-000001
status      : CONFIRMED
currency    : KWD
subtotal    : 33.000
discount    : 3.300
shipping    : 0.000
total       : 29.700
promotions  : {@{id=R2; description=WELCOME10 coupon; amount=3.300}}
lines       : {@{sku=MH-OUD-50; name=Royal Oud 50ml; quantity=2; unitPrice=12.500}, @{sku=MH-ROSE-30; name=Damask Rose 30ml; quantity=1; unitPrice=8.000}}
payment     : @{method=CARD; status=AUTHORISED; cardLast4=1111}
```

**Ship it**
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/orders/MH-2026-000001/transitions -ContentType "application/json" -Body '{"action":"SHIP"}'
```

Output:
```powershell
orderNumber : MH-2026-000001
status      : SHIPPED
currency    : KWD
subtotal    : 33.000
discount    : 3.300
shipping    : 0.000
total       : 29.700
promotions  : {@{id=R2; description=WELCOME10 coupon; amount=3.3}}
lines       : {@{sku=MH-OUD-50; name=Royal Oud 50ml; quantity=2; unitPrice=12.500}, @{sku=MH-ROSE-30; name=Damask Rose 30ml; quantity=1; unitPrice=8.000}}
payment     : @{method=CARD; status=AUTHORISED; cardLast4=1111}
```

**Search for a product, change its price, and search again**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/search/products?q=oud&size=1"
$body = '{"nameEn":"Royal Oud 100ml","nameAr":"\u0639\u0648\u062f \u0645\u0644\u0643\u064a 100 \u0645\u0644","brand":"MAISON","category":"fragrance","price":9.900,"status":"ACTIVE"}'
Invoke-RestMethod -Method Put -Uri http://localhost:8080/api/v1/admin/products/MH-OUD-100 -ContentType "application/json" -Body $body
Start-Sleep -Seconds 3
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/search/products?q=oud&size=1"
```

Output:
```powershell
content       : {@{sku=MH-OUD-100; name=Royal Oud 100ml; brand=MAISON; price=21.500}}
page          : 0
size          : 1
totalElements : 6
totalPages    : 6
facets        : @{brand=System.Object[]; category=System.Object[]}
```
```powershell
sku      : MH-OUD-100
nameEn   : Royal Oud 100ml
nameAr   : Ø¹Ù Ø¯ Ù Ù Ù Ù  100 Ù Ù
brand    : MAISON
category : fragrance
price    : 9.900
currency : KWD
status   : ACTIVE
version  : 1
```
```powershell
content       : {@{sku=MH-OUD-100; name=Royal Oud 100ml; brand=MAISON; price=9.900}}
page          : 0
size          : 1
totalElements : 6
totalPages    : 6
facets        : @{brand=System.Object[]; category=System.Object[]}
```

**Shipping options**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/shipping/options?country=KW"
```

Output:
```powershell
carrier       price currency estimatedDays
-------       ----- -------- -------------
GulfPost      1.500 KWD                  4
DesertExpress 2.500 KWD                  2
FastCourier   3.500 KWD                  1
```

## Pricing rules

The rules live in `src/main/resources/rules/pricing/` as Drools DRL files. Java builds the facts and reads the result, and never decides whether a promotion applies.

| Id | Rule | Detail |
|---|---|---|
| R1 | Basket tier | Subtotal of 20.000 KWD or more gets 5%, and 50.000 or more gets 10%. Only the highest tier applies. |
| R2 | Welcome coupon | `WELCOME10` gives 10% off, capped at 5.000 KWD, only for a customer with no earlier orders. |
| R3 | No stacking | R1 and R2 never stack. The customer gets the larger. |
| R4 | Shipping | Free for GOLD customers, or when subtotal minus discount is 15.000 or more. Otherwise 1.500. |
| F1 | Fraud hold | Cash on delivery with a total above 100.000 puts the order on hold for manual approval. |

## Design notes
**Persistence:** why `SINGLE_TABLE` for payments.
The payment kinds differ by only two or three columns, so one table wastes almost nothing.
Loading an order’s payment is a single table read with no join, whereas JOINED would add a join to every payment read.

### Design patterns:
**State.** What can be done with an order depends on its status. A confirmed order can be shipped, but a delivered order cannot be changed at all. Each status is therefore its own small class (`OnHold`, `Confirmed`, `Shipped`, `Delivered` and `Cancelled`), and each class overrides only the moves it allows. Any other move throws `IllegalTransitionException`, which the API returns as a 409. Without this pattern, every action such as `ship` or `cancel` would need its own `switch` on the status. Adding a new status would mean editing all of them, and one could easily be missed.

**Strategy and Factory.** Each payment method behaves differently. A card is authorised through the payment gateway, cash on delivery is only recorded as pending, and a wallet is charged directly. Each method has its own `PaymentStrategy`, and `PaymentStrategyFactory` collects them all and returns the right one for a given method. The order code never asks which method is being used. It receives a strategy and calls it. Without these patterns, the order code would need an `if` or `switch` on the payment method in several places, such as when paying and when refunding. A fourth method would then mean changing all of them. With the patterns, it means adding one new class.

**Adapter.** The fake payment vendor has an awkward SDK. `FakePayClient` has a single method that takes a `Map<String, String>`, expects amounts as text in fils, and answers with result codes such as `"00"` and `"51"`. `FakePayGateway` translates all of this into the project's own `PaymentGateway` interface, so the rest of the code never sees the vendor's types. Without it, those map keys and result codes would be spread across the payment code, and a change by the vendor would require edits in many places. With it, only one class needs to change.

**Builder.** Tests need orders in many shapes: shipped, on hold, with ten lines, and so on. `OrderFixtures.anOrder().withStatus(SHIPPED).build()` starts from sensible defaults and lets each test change only the detail it cares about. Without it, every test would repeat a long constructor call with many arguments, and a single change to the `Order` constructor would break many tests at once.

**Where a pattern was deliberately not used.** There is only one payment gateway implementation, so no factory or strategy was added for choosing between gateways. For a single option, that would be over-engineering. If a second provider is ever added, that is the time to introduce one.

### Events and delivery guarantees:
**Why an outbox.** MySQL and Kafka can't be committed together. Writing to Kafka inside the transaction risks announcing a rolled-back order; writing after risks losing the event on a crash. Instead, each change writes an outbox_event row in the same transaction. OutboxRelay publishes those rows separately.

**Demonstration: stopping the app before the relay runs loses nothing.** The relay was slowed to one run every 10 minutes (`MARKETHUB_OUTBOX_POLL_INTERVAL=PT10M`) and an order was placed. Its two outbox rows were in MySQL with `published_at` empty, and Kafka held no message for the order. The application was then stopped and started again with the normal interval.

Before the restart, the outbox rows:
```
event_type      aggregate_id    published_at
OrderPlaced     MH-2026-000002  NULL
OrderConfirmed  MH-2026-000002  NULL
```
After the restart, the same rows and the messages found in Kafka:
```
event_type      published_at
OrderPlaced     2026-11-16 08:53:23.820852
OrderConfirmed  2026-11-16 08:53:23.842401

type           aggregateId    occurredAt
----           -----------    ----------
OrderPlaced    MH-2026-000002 2026-11-16T08:50:48.330Z
OrderConfirmed MH-2026-000002 2026-11-16T08:50:48.345Z
```
The events were published on restart, in the order they were written. This works because an order and its outbox rows are committed in one transaction, so a crash can only happen before both exist or after both exist. The relay sends every row that has no `published_at`, so it picks up whatever the previous run left behind.

**Delivery is at-least-once.** The relay marks a row published only after Kafka acknowledges it. If the app stops in between, the row is sent again on the next run. No event is lost, but duplicates are possible, so consumers must tolerate them.

**What idempotence protects against.** With enable.idempotence=true, the producer tags each message with a producer id and sequence number. On retry, the broker recognises the repeat and keeps one copy, preserving order within a partition. With acks=all, an acknowledged message is stored by all in-sync replicas.

**What it does not protect against.** Idempotence only covers retries within one producer session. A restart gives the producer a new identity, so the broker sees a new message. It also doesn't prevent duplicates from two relay instances or a consumer replaying after a failure. Consumers handle this by ignoring events they've already seen.

**Ordering.** Kafka only keeps order within a partition, and the message key picks the partition. Order events use the order number; product events use the SKU. The relay sends one event at a time, oldest first, stopping at the first failure so a later event can never overtake an earlier one.

### Events and concurrency decisions:

**1. Delivery.** The outbox gives at-least-once delivery. The relay marks a row as published only after Kafka has acknowledged it, so a crash between the send and that update makes the relay send the event again on the next run. Duplicates will happen, and consumers must not care: they have to produce the same result when they see the same event twice.

**2. Ordering.** Kafka keeps order only within a partition, and the message key chooses the partition. Order events are keyed by order number and product events by SKU, so all events of one order or product arrive in the order they were written. No consumer may depend on global order across different keys, and none does.

**3. Freshness.** Search lags behind MySQL by a second or two, because a change travels through the outbox, Kafka and the Elasticsearch refresh interval before it is visible. Tests wait for it with Awaitility instead of sleeping.

**4. Failure (new).** A failing product event is retried three times with a growing pause (0.5 s, 1 s, 2 s) and then moved to the dead-letter topic `markethub.products.v1.DLT`. A message that cannot even be parsed skips the retries and goes to the dead-letter topic immediately. One bad message therefore never blocks a partition, because the consumer moves on to the next message after quarantining it.

**5. Rebuild.** Search is a copy of the data in MySQL, never the source. If the Elasticsearch data is deleted, the application recreates the index on the next start and reloads every product from MySQL with the bulk API.

**6. Thread safety.** See the table in the concurrency section. In short: `KieContainer`, the Elasticsearch client and `KafkaTemplate` are shared because they are built for concurrent use. `KieSession` and managed JPA entities are never shared, because they belong to one request or transaction.

**When Kafka is the wrong answer (new).** Kafka is for work that can happen later and that nobody is waiting for. It is the wrong tool when the caller needs the result now to continue, which is why pricing is a synchronous call and not an event. It is also the wrong tool when correctness depends on the result, such as taking a payment: an order must not be confirmed before the payment succeeds, so payment is a direct call inside the order transaction. Events are used for announcing what already happened (an order was placed, a product changed), where a delay of a second or two does no harm.

### Search:
**Why price is not a float.** Floating point numbers cannot represent most decimals exactly, so a price such as 9.900 could be stored slightly off and give wrong results at the edges of a price filter. Prices are indexed as `scaled_float` with a factor of 1000: Elasticsearch stores the price times 1000 as a whole number, which is exact for KWD's three decimals.

**Freshness.** Search lags behind MySQL by a second or two. A product change travels from MySQL through the outbox and Kafka to Elasticsearch, and becomes visible after the next index refresh. Tests wait for this with Awaitility instead of sleeping.

**Search is a copy, never the source.** If the Elasticsearch data is deleted, the application recreates the index and reloads every product from MySQL on the next start.

### Concurrency:
**Why blocking calls must not run on the common ForkJoinPool.** `CompletableFuture.supplyAsync` without an executor runs on `ForkJoinPool.commonPool()`. That pool is shared by the whole JVM, including parallel streams, and it is sized to roughly the number of CPU cores minus one. It is meant for short CPU-bound work. A carrier call spends its time waiting on the network, and a waiting thread still occupies its slot, so a few slow carriers can use up the whole pool and stall unrelated work. Shipping quotes therefore run on a dedicated, bounded `ThreadPoolExecutor` with named threads (`carrier-1`, `carrier-2`, ...). A slow carrier can only exhaust that pool, the bounded queue limits the backlog, and log lines show which thread did the work.

**Platform pool vs virtual threads.** Carrier calls can run on a bounded pool of platform threads (the default) or on virtual threads (`markethub.carriers.executor=virtual`). A virtual thread that blocks on the network is parked by the JVM at almost no cost, so a burst of waiting calls does not need a thread each from a small pool. A comparison test sends 100 shipping requests at once (300 carrier calls of 200 ms each). The bounded pool of 12 threads and a queue of 100 answered 36 of 300 calls and left 64 requests without any answer, while virtual threads answered all 300 in 323 ms. The trade-off is that virtual threads have no built-in limit: a burst creates as many threads as there are calls, so the bounded pool stays the default, and a production setup using virtual threads would cap the concurrency explicitly, for example with a semaphore. Virtual threads help with waiting, not with CPU-heavy work.

### Payment retries (decorator).
`RetryingPaymentGateway` wraps the payment gateway and repeats a call that timed out, up to three attempts with a short, growing pause. A decline is never retried: it is an answer from the bank, so asking again cannot change it and could lock the card. Anything that is not a timeout also fails immediately. If every attempt times out, the order transaction rolls back and the API answers 503 `PAYMENT_TIMEOUT`; the client can safely retry with the same `Idempotency-Key`. Two limits are worth knowing. The calls run inside the order's database transaction, so the pauses are short on purpose. And with a real provider, a retried charge must reuse the same provider-side reference, otherwise a request that timed out after being processed could be charged twice. The fake provider has no side effects, so this project does not need that.

## Testing

| Level | What it covers | Tools |
|---|---|---|
| Unit and rules | Services with mocked repositories, the state classes, money rounding, every pricing rule with its boundaries | JUnit 5, Mockito, AssertJ, Drools without Spring |
| Slice | HTTP validation, status codes and the error shape (`@WebMvcTest`), queries, constraints and the projection (`@DataJpaTest`) | Spring test slices |
| Integration | The whole app against real MySQL, Kafka and Elasticsearch containers, including the two concurrency tests | Testcontainers, Awaitility |

Run everything with `./mvnw verify`. Docker must be running. The build fails if line coverage on `domain` and `application` drops below 80% or branch coverage below 70%.

Coverage at the time of writing: 96% line, 78% branch (total); domain and application both exceed the 80% line / 70% branch minimums. The full suite takes about 01:29 min.

## Known limitations

- **No authentication.** The service is unauthenticated and not safe to expose to a network as it is.
- **Search lags by a second or two** behind MySQL, because changes travel through the outbox and Kafka.
- **Deep paging** in search is limited by Elasticsearch to 10,000 results and currently returns a server error beyond that.
- **One relay instance.** Two copies of the app could publish the same outbox row twice, which consumers must tolerate anyway.
- **Rare deadlocks.** If the first of many identical order requests fails (for example a declined card), MySQL can report a deadlock to the others instead of a clean duplicate.
- **Request ids** are not copied to the carrier threads, so those log lines have an empty id.
- **Next steps:** authentication, a dead-letter replay tool, and a guarded deep-paging response.

