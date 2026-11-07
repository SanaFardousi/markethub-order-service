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
