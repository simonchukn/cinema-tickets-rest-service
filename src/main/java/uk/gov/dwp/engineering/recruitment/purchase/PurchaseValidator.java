package uk.gov.dwp.engineering.recruitment.purchase;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

@Component
public class PurchaseValidator {

  private static final int MAX_TICKETS = 25;

  public TicketCounts validate(final Long accountId, final TicketRequest... ticketRequests) {

    // AC3: account id must be above zero
    if (accountId == null || accountId <= 0) {
      throw new InvalidBookingException("Account id must be greater than zero");
    }

    final TicketCounts counts = countTickets(ticketRequests);

    // AC5: at least one adult
    if (counts.adults() == 0) {
      throw new InvalidBookingException("At least one adult ticket is required");
    }

    // AC6: one infant per adult lap
    if (counts.infants() > counts.adults()) {
      throw new InvalidBookingException("Each infant needs an adult to sit with");
    }

    return counts;
  }

  private static TicketCounts countTickets(final TicketRequest... ticketRequests) {

    // AC8: ticket list must be present
    if (ticketRequests == null) {
      throw new InvalidBookingException("Ticket requests are required");
    }

    int adults = 0;
    int children = 0;
    int infants = 0;

    for (final TicketRequest request : ticketRequests) {
      // AC8: every line needs a ticket type
      if (request == null || request.type() == null) {
        throw new InvalidBookingException("Every ticket request needs a ticket type");
      }

      final int count = request.ticketCount();

      // AC4: no negative quantities
      if (count < 0) {
        throw new InvalidBookingException("Ticket quantities cannot be negative");
      }

      // AC7: no more than 25 tickets, checked before adding so the total never overflows
      if (count > MAX_TICKETS - (adults + children + infants)) {
        throw new InvalidBookingException(
            "No more than " + MAX_TICKETS + " tickets can be bought at once");
      }

      // Same ticket type on several lines is added together
      switch (request.type()) {
        case ADULT -> adults += count;
        case CHILD -> children += count;
        case INFANT -> infants += count;
      }
    }

    return new TicketCounts(adults, children, infants);
  }
}
