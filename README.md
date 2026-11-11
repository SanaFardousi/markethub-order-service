# MarketHub Order & Search Service

## Design notes

**Why is `SINGLE_TABLE` preferred over `JOINED` inheritance for the Payment hierarchy?**
- The payment kinds differ by only two or three columns, so one table wastes almost nothing.
- Loading an order’s payment is a single table read with no join, whereas JOINED would add a join to every payment read.

## Design patterns

**State.** What can be done with an order depends on its status. A confirmed order can be shipped, but a delivered order cannot be changed at all. Each status is therefore its own small class (`OnHold`, `Confirmed`, `Shipped`, `Delivered` and `Cancelled`), and each class overrides only the moves it allows. Any other move throws `IllegalTransitionException`, which the API returns as a 409. Without this pattern, every action such as `ship` or `cancel` would need its own `switch` on the status. Adding a new status would mean editing all of them, and one could easily be missed.

**Strategy and Factory.** Each payment method behaves differently. A card is authorised through the payment gateway, cash on delivery is only recorded as pending, and a wallet is charged directly. Each method has its own `PaymentStrategy`, and `PaymentStrategyFactory` collects them all and returns the right one for a given method. The order code never asks which method is being used. It receives a strategy and calls it. Without these patterns, the order code would need an `if` or `switch` on the payment method in several places, such as when paying and when refunding. A fourth method would then mean changing all of them. With the patterns, it means adding one new class.

**Adapter.** The fake payment vendor has an awkward SDK. `FakePayClient` has a single method that takes a `Map<String, String>`, expects amounts as text in fils, and answers with result codes such as `"00"` and `"51"`. `FakePayGateway` translates all of this into the project's own `PaymentGateway` interface, so the rest of the code never sees the vendor's types. Without it, those map keys and result codes would be spread across the payment code, and a change by the vendor would require edits in many places. With it, only one class needs to change.

**Builder.** Tests need orders in many shapes: shipped, on hold, with ten lines, and so on. `OrderFixtures.anOrder().withStatus(SHIPPED).build()` starts from sensible defaults and lets each test change only the detail it cares about. Without it, every test would repeat a long constructor call with many arguments, and a single change to the `Order` constructor would break many tests at once.

**Where a pattern was deliberately not used.** There is only one payment gateway implementation, so no factory or strategy was added for choosing between gateways. For a single option, that would be over-engineering. If a second provider is ever added, that is the time to introduce one.

## Events and delivery guarantees

**Why an outbox.** MySQL and Kafka can't be committed together. Writing to Kafka inside the transaction risks announcing a rolled-back order; writing after risks losing the event on a crash. Instead, each change writes an outbox_event row in the same transaction. OutboxRelay publishes those rows separately.

**Delivery is at-least-once.** The relay marks a row published only after Kafka acknowledges it. If the app stops in between, the row is sent again on the next run. No event is lost, but duplicates are possible, so consumers must tolerate them.

**What idempotence protects against.** With enable.idempotence=true, the producer tags each message with a producer id and sequence number. On retry, the broker recognises the repeat and keeps one copy, preserving order within a partition. With acks=all, an acknowledged message is stored by all in-sync replicas.

**What it does not protect against.** Idempotence only covers retries within one producer session. A restart gives the producer a new identity, so the broker sees a new message. It also doesn't prevent duplicates from two relay instances or a consumer replaying after a failure. Consumers handle this by ignoring events they've already seen.

**Ordering.** Kafka only keeps order within a partition, and the message key picks the partition. Order events use the order number; product events use the SKU. The relay sends one event at a time, oldest first, stopping at the first failure so a later event can never overtake an earlier one.

## Search

**Why price is not a float.** Floating point numbers cannot represent most decimals exactly, so a price such as 9.900 could be stored slightly off and give wrong results at the edges of a price filter. Prices are indexed as `scaled_float` with a factor of 1000: Elasticsearch stores the price times 1000 as a whole number, which is exact for KWD's three decimals.

**Freshness.** Search lags behind MySQL by a second or two. A product change travels from MySQL through the outbox and Kafka to Elasticsearch, and becomes visible after the next index refresh. Tests wait for this with Awaitility instead of sleeping.

**Search is a copy, never the source.** If the Elasticsearch data is deleted, the application recreates the index and reloads every product from MySQL on the next start.
