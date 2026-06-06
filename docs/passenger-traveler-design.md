# Passenger Traveler Profile Design

## Goal

Passenger traveler profiles provide reusable real-name passenger information for the passenger ticketing side. A logged-in `USER` can maintain a private traveler list, select one traveler during booking, and let the system snapshot masked identity information into orders and electronic tickets.

## Data Model

`passenger_profiles` stores common traveler information owned by a passenger account.

| Field | Description |
| --- | --- |
| `id` | Primary key |
| `user_id` | Owner passenger user ID |
| `passenger_name` | Traveler name |
| `id_type` | Identity type: `ID_CARD`, `PASSPORT`, `OTHER` |
| `id_no` | Full identity number, stored for booking validation and snapshot generation |
| `phone` | Optional traveler phone |
| `default_traveler` | Whether this traveler is the default option |
| `created_at` / `updated_at` | Audit timestamps |

The repository enforces duplicate checks by `user_id + passenger_name + id_type + id_no`. Each passenger can set one default traveler; setting a new default clears the previous default for the same user.

## Passenger API

All endpoints require a `USER` token and operate only on the current logged-in passenger.

```http
GET    /api/passenger/travelers
POST   /api/passenger/travelers
PUT    /api/passenger/travelers/{id}
DELETE /api/passenger/travelers/{id}
POST   /api/passenger/travelers/{id}/default
```

Create and update request body:

```json
{
  "passengerName": "Zhang San",
  "idType": "ID_CARD",
  "idNo": "110101200001010011",
  "phone": "13800010001",
  "defaultTraveler": true
}
```

Response uses masked sensitive fields:

```json
{
  "id": 1,
  "passengerName": "Zhang San",
  "idType": "ID_CARD",
  "idNoMasked": "110***********0011",
  "phoneMasked": "138****0001",
  "defaultTraveler": true
}
```

## Booking Flow

`POST /api/passenger/orders` now supports two input modes:

1. `travelerId`: use an existing traveler profile owned by the current passenger.
2. Manual passenger fields: `passengerName`, `passengerIdCard`, `passengerIdType`, `passengerPhone`.

When `travelerId` is provided, the service loads the traveler by `id + current userId`. Other passengers cannot use or modify that traveler. The order still reuses the existing `OrderService` state machine, inventory lock and `requestId` idempotency rules.

## Snapshot Rule

Orders and electronic tickets keep immutable passenger snapshots:

- `TicketOrder.passengerName`
- `TicketOrder.passengerIdType`
- `TicketOrder.passengerIdNoMasked`
- `TicketOrder.passengerPhoneMasked`
- `TicketRecord.passengerName`
- `TicketRecord.passengerIdType`
- `TicketRecord.passengerIdCardMasked`
- `TicketRecord.passengerPhoneMasked`

This avoids historical order and ticket details changing when the passenger later edits or deletes a common traveler profile.

## Frontend Integration

`frontend/passenger.html` adds a traveler management section and a traveler selector in the booking modal. `frontend/passenger.js` loads traveler profiles after login, supports create/update/delete/default actions, and uses `travelerId` for booking when a saved traveler is selected.

Manual passenger entry is still available for one-off bookings.

## Security Boundary

- A `USER` can only list, create, update, delete and set default traveler profiles under their own `userId`.
- A `USER` cannot create an order using another passenger's `travelerId`.
- API responses never return full identity numbers or full phone numbers.
- Management-side order and ticket responses expose masked snapshot fields only.
