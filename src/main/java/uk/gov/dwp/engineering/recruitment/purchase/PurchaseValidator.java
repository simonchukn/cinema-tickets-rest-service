package uk.gov.dwp.engineering.recruitment.purchase;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

@Component
public class PurchaseValidator {

  public TicketCounts validate(final Long accountId, final TicketRequest... ticketRequests) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
