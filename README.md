# MarketHub Order & Search Service

[![CI](https://github.com/SanaFardousi/markethub-order-service/actions/workflows/ci.yml/badge.svg)](https://github.com/SanaFarodusi/markethub-order-service/actions/workflows/ci.yml)

A Spring Boot service for a Gulf e-commerce store. It prices baskets with Drools rules, takes orders and moves them through a lifecycle built on design patterns, publishes every order and product change to Kafka through a transactional outbox, serves product search from Elasticsearch, and stays correct under concurrent requests. Money is in Kuwaiti dinars with three decimals.

## Quick start

You need Docker (at least 6 GB of memory) and nothing else.

```bash
git clone https://github.com/<your-user>/markethub-order-service.git
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
```bash
curl -s -X POST localhost:8080/api/v1/pricing/preview -H 'Content-Type: application/json' \
  -d '{"customerId":1,"couponCode":"WELCOME10","paymentMethod":"CARD","lines":[{"sku":"MH-OUD-50","quantity":2},{"sku":"MH-ROSE-30","quantity":1}]}'
```
Output: *(paste the real output)*

**Place an order**
```bash
curl -i -X POST localhost:8080/api/v1/orders -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 3f6c0e2a-8d4b-4c1e-9a77-1b2c3d4e5f60' \
  -d '{"customerId":1,"couponCode":"WELCOME10","paymentMethod":"CARD","card":{"number":"4111111111111111","expiry":"12/28"},"shippingAddress":{"country":"KW","city":"Kuwait City","line1":"Block 3, Street 12"},"lines":[{"sku":"MH-OUD-50","quantity":2},{"sku":"MH-ROSE-30","quantity":1}]}'
```
Output: *(paste the real output)*

**Ship it** (use the order number from the previous output)
```bash
curl -s -X POST localhost:8080/api/v1/orders/MH-2026-000001/transitions -H 'Content-Type: application/json' -d '{"action":"SHIP"}'
```
Output: *(paste)*

**Search for a product, change its price, and search again**
```bash
curl -s 'localhost:8080/api/v1/search/products?q=oud&size=1'
curl -s -X PUT localhost:8080/api/v1/admin/products/MH-OUD-50 -H 'Content-Type: application/json' \
  -d '{"nameEn":"Royal Oud 50ml","nameAr":"عود ملكي 50 مل","brand":"MAISON","category":"fragrance","price":9.900,"status":"ACTIVE"}'
sleep 3
curl -s 'localhost:8080/api/v1/search/products?q=oud&size=1'
```
Output: *(paste all three)*

**Shipping options**
```bash
curl -s 'localhost:8080/api/v1/shipping/options?country=KW'
```
Output: *(paste)*

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

**Design patterns:**
**State.** What can be done with an order depends on its status. A confirmed order can be shipped, but a delivered order cannot be changed at all. Each status is therefore its own small class (`OnHold`, `Confirmed`, `Shipped`, `Delivered` and `Cancelled`), and each class overrides only the moves it allows. Any other move throws `IllegalTransitionException`, which the API returns as a 409. Without this pattern, every action such as `ship` or `cancel` would need its own `switch` on the status. Adding a new status would mean editing all of them, and one could easily be missed.

**Strategy and Factory.** Each payment method behaves differently. A card is authorised through the payment gateway, cash on delivery is only recorded as pending, and a wallet is charged directly. Each method has its own `PaymentStrategy`, and `PaymentStrategyFactory` collects them all and returns the right one for a given method. The order code never asks which method is being used. It receives a strategy and calls it. Without these patterns, the order code would need an `if` or `switch` on the payment method in several places, such as when paying and when refunding. A fourth method would then mean changing all of them. With the patterns, it means adding one new class.

**Adapter.** The fake payment vendor has an awkward SDK. `FakePayClient` has a single method that takes a `Map<String, String>`, expects amounts as text in fils, and answers with result codes such as `"00"` and `"51"`. `FakePayGateway` translates all of this into the project's own `PaymentGateway` interface, so the rest of the code never sees the vendor's types. Without it, those map keys and result codes would be spread across the payment code, and a change by the vendor would require edits in many places. With it, only one class needs to change.

**Builder.** Tests need orders in many shapes: shipped, on hold, with ten lines, and so on. `OrderFixtures.anOrder().withStatus(SHIPPED).build()` starts from sensible defaults and lets each test change only the detail it cares about. Without it, every test would repeat a long constructor call with many arguments, and a single change to the `Order` constructor would break many tests at once.

**Where a pattern was deliberately not used.** There is only one payment gateway implementation, so no factory or strategy was added for choosing between gateways. For a single option, that would be over-engineering. If a second provider is ever added, that is the time to introduce one.

**Events and delivery guarantees:**
**Why an outbox.** MySQL and Kafka can't be committed together. Writing to Kafka inside the transaction risks announcing a rolled-back order; writing after risks losing the event on a crash. Instead, each change writes an outbox_event row in the same transaction. OutboxRelay publishes those rows separately.

**Delivery is at-least-once.** The relay marks a row published only after Kafka acknowledges it. If the app stops in between, the row is sent again on the next run. No event is lost, but duplicates are possible, so consumers must tolerate them.

**What idempotence protects against.** With enable.idempotence=true, the producer tags each message with a producer id and sequence number. On retry, the broker recognises the repeat and keeps one copy, preserving order within a partition. With acks=all, an acknowledged message is stored by all in-sync replicas.

**What it does not protect against.** Idempotence only covers retries within one producer session. A restart gives the producer a new identity, so the broker sees a new message. It also doesn't prevent duplicates from two relay instances or a consumer replaying after a failure. Consumers handle this by ignoring events they've already seen.

**Ordering.** Kafka only keeps order within a partition, and the message key picks the partition. Order events use the order number; product events use the SKU. The relay sends one event at a time, oldest first, stopping at the first failure so a later event can never overtake an earlier one.

**Search:**
**Why price is not a float.** Floating point numbers cannot represent most decimals exactly, so a price such as 9.900 could be stored slightly off and give wrong results at the edges of a price filter. Prices are indexed as `scaled_float` with a factor of 1000: Elasticsearch stores the price times 1000 as a whole number, which is exact for KWD's three decimals.

**Freshness.** Search lags behind MySQL by a second or two. A product change travels from MySQL through the outbox and Kafka to Elasticsearch, and becomes visible after the next index refresh. Tests wait for this with Awaitility instead of sleeping.

**Search is a copy, never the source.** If the Elasticsearch data is deleted, the application recreates the index and reloads every product from MySQL on the next start.

**Concurrency:**
**Why blocking calls must not run on the common ForkJoinPool.** `CompletableFuture.supplyAsync` without an executor runs on `ForkJoinPool.commonPool()`. That pool is shared by the whole JVM, including parallel streams, and it is sized to roughly the number of CPU cores minus one. It is meant for short CPU-bound work. A carrier call spends its time waiting on the network, and a waiting thread still occupies its slot, so a few slow carriers can use up the whole pool and stall unrelated work. Shipping quotes therefore run on a dedicated, bounded `ThreadPoolExecutor` with named threads (`carrier-1`, `carrier-2`, ...). A slow carrier can only exhaust that pool, the bounded queue limits the backlog, and log lines show which thread did the work.

## Testing

| Level | What it covers | Tools |
|---|---|---|
| Unit and rules | Services with mocked repositories, the state classes, money rounding, every pricing rule with its boundaries | JUnit 5, Mockito, AssertJ, Drools without Spring |
| Slice | HTTP validation, status codes and the error shape (`@WebMvcTest`), queries, constraints and the projection (`@DataJpaTest`) | Spring test slices |
| Integration | The whole app against real MySQL, Kafka and Elasticsearch containers, including the two concurrency tests | Testcontainers, Awaitility |

Run everything with `./mvnw verify`. Docker must be running. The build fails if line coverage on `domain` and `application` drops below 80% or branch coverage below 70%.

Coverage at the time of writing: *(line % and branch % from `target/site/jacoco/index.html`)*. The full suite takes about *(your time)*.

## Known limitations

- **No authentication.** The service is unauthenticated and not safe to expose to a network as it is.
- **Search lags by a second or two** behind MySQL, because changes travel through the outbox and Kafka.
- **Deep paging** in search is limited by Elasticsearch to 10,000 results and currently returns a server error beyond that.
- **One relay instance.** Two copies of the app could publish the same outbox row twice, which consumers must tolerate anyway.
- **Rare deadlocks.** If the first of many identical order requests fails (for example a declined card), MySQL can report a deadlock to the others instead of a clean duplicate.
- **Request ids** are not copied to the carrier threads, so those log lines have an empty id.
- **Next steps:** authentication, a dead-letter replay tool, and a guarded deep-paging response.

