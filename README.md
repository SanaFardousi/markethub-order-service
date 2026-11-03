# MarketHub Order & Search Service

## Design notes

**Why is `SINGLE_TABLE` preferred over `JOINED` inheritance for the Payment hierarchy?**
- The payment kinds differ by only two or three columns, so one table wastes almost nothing.
- Loading an order’s payment is a single table read with no join, whereas JOINED would add a join to every payment read.
