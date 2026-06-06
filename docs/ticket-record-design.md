# Ticket Record and Order Detail Design

## Design Goal

The system now records an electronic ticket after an order is paid. The ticket record separates the passenger itinerary view from the order state machine, while still keeping both models connected by `orderId` and `orderNo`.

This design supports two usage paths:

- Passenger side: passengers can open an order detail page and view the itinerary, ticket number, payment records, and refund records that belong to the current user.
- Admin side: operators can open an order detail page and inspect the order, ticket, payments, refunds, risk events, Outbox events, and recent operation logs in one place.

## Ticket Status

`TicketStatus` contains three states:

- `ISSUED`: the order has been paid and the electronic ticket is valid.
- `REFUNDED`: the order has been refunded and the ticket has been invalidated.
- `CANCELLED`: reserved for a ticket that was issued and then cancelled by another business path.

The current order state machine remains unchanged:

```text
PENDING_PAYMENT -> PAID
PENDING_PAYMENT -> CLOSED
PAID -> REFUNDED
```

Ticket status is derived from business actions and does not introduce a new order state.

## Issue Ticket Flow

When a `PENDING_PAYMENT` order is paid successfully:

1. The order becomes `PAID`.
2. Payment success logic continues to trigger risk rules, cache invalidation, operation logs, and Outbox events.
3. `TicketService.issueTicketForPaidOrder` creates one `TicketRecord` for the order.
4. The ticket stores train number, stations, travel date, departure and arrival time, seat type, passenger name, masked ID card, amount, and issued time.
5. Repeated payment calls return the existing paid result and do not create duplicate tickets.

`ticket_records.order_id` is unique, so one order maps to at most one electronic ticket.

## Refund Ticket Flow

When a `PAID` order is refunded:

1. The order becomes `REFUNDED`.
2. Inventory is released.
3. Refund record creation remains unchanged.
4. The matching `TicketRecord` is updated to `REFUNDED` and `invalidatedAt` is recorded.
5. If a historical paid order does not yet have a ticket record, the refund flow creates the missing ticket first and then invalidates it. This keeps demo and legacy data consistent.

Refunding a ticket does not repeat inventory release or duplicate refund records; it only updates the ticket validity record.

## Order Detail API

Passenger order detail:

```http
GET /api/passenger/orders/{id}/detail
Authorization: Bearer {USER token}
```

The passenger API validates ownership before returning data. It returns:

- order
- ticket
- payment records
- refund records

It does not expose risk events, Outbox events, or operation logs.

Admin order detail:

```http
GET /api/orders/{id}/detail
Authorization: Bearer {ADMIN | OPERATOR | RISK_OFFICER token}
```

The admin API returns:

- order
- ticket
- payment records
- refund records
- related risk events
- related Outbox events
- recent operation logs

This gives the management side a single transaction trace view.

## Data Consistency

The ticket record is created inside the payment success transaction and invalidated inside the refund transaction. This keeps order status, ticket validity, payment/refund records, risk triggering, operation logs, and Outbox events aligned.

The implementation is intentionally incremental: it adds ticket records and order detail aggregation without changing order states, payment callback verification, refund callback verification, or passenger ownership rules.

