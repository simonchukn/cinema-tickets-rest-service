# Cinema Tickets Code

## Requirements Analysis

Following a review of the business rules, constraints and assumptions, these were turned into a list of acceptance criteria, each of which can be checked by a test. Some rules can be read more than one way, for example whether infants count towards the 25 ticket limit. For each of these, a decision was made and the reasoning recorded, so the behaviour is deliberate rather than accidental. The code was built test first, in small commits that each trace back to one or more of the criteria below.

## Acceptance Criteria (AC)

1.  AC1: The total price is the sum of each ticket type's count times its price. ADULT costs 25.99 GBP, CHILD costs 17.50 GBP and INFANT is free.
2.  AC2: The number of seats reserved is the number of ADULT tickets plus the number of CHILD tickets. Infants never get a seat.
3.  AC3: A request is rejected if the account id is missing, zero or negative.
4.  AC4: A request is rejected if any ticket quantity is negative.
5.  AC5: A request is rejected unless it contains at least one ADULT ticket.
6.  AC6: A request is rejected if there are more INFANT tickets than ADULT tickets.
7.  AC7: A request is rejected if the total number of tickets, infants included, is more than 25.
8.  AC8: A request is rejected if it has no ticket list at all, if the list contains an empty entry, or if a ticket line does not say which ticket type it is for. A request does not need to include every ticket type, so ADULT tickets on their own are valid.
9.  AC9: A valid request makes exactly one payment request to the PaymentService, for the requesting account and the total price.
10. AC10: A valid request makes exactly one seat reservation request to the SeatReservationService, for the requesting account and the seat count, after the payment request.
11. AC11: A rejected request makes no calls to the PaymentService or the SeatReservationService.
12. AC12: The endpoint returns 201 with the account id, seat count and total cost for a valid booking, and 400 with a problem detail body for a rejected or malformed request.

## Assumptions and Ambiguity Resolution

-   The 25 ticket limit counts infants and applies to a single request. The rule says tickets, not seats, and an infant still holds a ticket. So 25 is allowed and 26 is rejected.
-   Infants cannot outnumber adults. Each infant sits on an adult's lap.
-   A ticket line with a quantity of zero is allowed and adds nothing. It is harmless, so ADULT 2, CHILD 0, INFANT 0 is a valid request.
-   Negative quantities are rejected. They would reduce the price and the seat count, which could never be correct.
-   An empty request is rejected. It falls under the rule that at least one adult ticket is needed, so it needs no special handling.
-   If the same ticket type appears more than once in a request, the quantities are added together and the rules apply to the combined total. Otherwise a buyer could get round the limits by splitting a line in two.
-   A missing account id is treated like an invalid one. Only ids greater than zero are valid, and a missing id is not greater than zero.
-   Invalid requests never reach the PaymentService or the SeatReservationService. Nothing should be charged or reserved for a request that will be refused.
-   Payment is taken before seats are reserved. The task lists payment first, and both services are assumed never to fail.
-   There is no error handling around the PaymentService or SeatReservationService. The spec says both services have no defects.
-   BookingConfirmation is extended with the seat count and total cost, and its original one argument constructor is kept. The OpenAPI file already lists both fields as required in the response, and keeping the old constructor means nothing that used it breaks.
-   The JSON request uses the field names from the domain records (accountId, ticketRequests, type, ticketCount). The domain package cannot be changed, so the OpenAPI file was updated to match the code instead.
-   Rejected purchases and malformed input, such as an unknown ticket type or a missing body, all return HTTP 400 with the same problem detail body. Callers then have one error format to deal with.

## Design Intent

-   CinemaTicketsServiceImpl validates the request, works out the price and seat count, asks for payment and then reserves the seats.
-   Validation lives in PurchaseValidator, which also adds up the tickets into a small TicketCounts record. Pricing lives in TicketPriceCalculator, and the seat count is a method on TicketCounts. Each can be read and tested on its own.
-   All dependencies are passed in through constructors.
-   Money is held as BigDecimal, because that is what the PaymentService takes, and it avoids rounding errors.
-   Rejections throw the existing InvalidBookingException with a message saying which rule failed. The CinemaTicketsService interface already declares it, so no new exception type is needed.
-   The existing RestExceptionHandler, a ControllerAdvice, maps InvalidBookingException to a 400 problem detail response. The controller only maps the request in and the response out.

## OpenAPI Specification

The API is described in `src/main/resources/cinema-tickets.yaml`. The only change is to the request fields, so they match the domain records: `tickets` is now `ticketRequests`, and `count` is now `ticketCount` (int32, to match the record's `int`).

The spec also backs up some of the rules: `accountId` starts at 1 (AC3), `ticketCount` at 0 (AC4), `seatCount` runs from 1 to 25 (AC5, AC7), and only 201 and 400 responses are listed (AC12).

## Test Strategy

-   The service is tested with Mockito mocks of both third party services. An ArgumentCaptor checks the exact account id, amount and seat count that were sent.
-   Prices, seat counts and the validation boundaries are covered by parameterised tests, for example 24, 25 and 26 tickets, and infants equal to and one more than adults.
-   Every rejection path checks that neither the PaymentService nor the SeatReservationService was called, using verifyNoInteractions.
-   The controller is tested with MockMvc for the happy path, one rejection for each validation rule, and malformed input. It uses the standalone setup with the real service, validator, calculator and error handler, so only the two third party services are mocked.
-   Test names refer to the acceptance criteria they cover.

## Commit History Note

The test commits fail on purpose. Each one adds tests and just enough stub code to compile, and the next feature commit makes them pass.

The one exception is the controller tests. The happy path and the malformed input tests passed as soon as they were written, because the controller already existed and Spring already returns a 400 for JSON it cannot read. Only the rejection tests failed until the error handler was changed.

## Running the Project

JDK 21 or later. Project includes Maven wrapper, hence Maven does not need to be installed.

Run the tests: `./mvnw test` or use `mvnw.cmd test` on Windows instead.

A GitHub Actions workflow (`.github/workflows/ci.yml`) runs `./mvnw verify` on Java 21 for every push and pull request to main. 

Start the service on port 8080:

```shell
./mvnw spring-boot:run
```

The payment and seat reservation services are stubs that always succeed, so the service can be tried locally without anything else running.

## Example Requests

A valid booking for 2 adults, 1 child and 1 infant:

```shell
curl -i -X POST localhost:8080/cinema/bookings \
  -H "Content-Type: application/json" \
  -d '{"accountId":1,"ticketRequests":[{"type":"ADULT","ticketCount":2},{"type":"CHILD","ticketCount":1},{"type":"INFANT","ticketCount":1}]}'
```

This returns 201:

```json
{ "accountId": 1, "seatCount": 3, "totalCost": 69.48 }
```

A rejected booking, with more infants than adults:

```shell
curl -i -X POST localhost:8080/cinema/bookings \
  -H "Content-Type: application/json" \
  -d '{"accountId":1,"ticketRequests":[{"type":"ADULT","ticketCount":1},{"type":"INFANT","ticketCount":2}]}'
```

This returns 400:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Each infant needs an adult to sit with",
  "instance": "/cinema/bookings"
}
```

## Project Structure

-   CinemaTicketsController receives the booking and returns the confirmation.
-   CinemaTicketsServiceImpl validates, prices, pays and reserves, in that order.
-   purchase/PurchaseValidator checks every rule and returns the ticket counts.
-   purchase/TicketCounts holds the adult, child and infant counts and works out the seats.
-   purchase/TicketPriceCalculator works out the total cost.
-   exception/RestExceptionHandler turns rejected bookings into a 400 response.
