package uk.gov.dwp.engineering.recruitment.purchase;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class TicketPriceCalculator {

  private static final BigDecimal ADULT_PRICE = new BigDecimal("25.99");
  private static final BigDecimal CHILD_PRICE = new BigDecimal("17.50");
  private static final BigDecimal INFANT_PRICE = new BigDecimal("0.00");

  public BigDecimal totalCost(final TicketCounts counts) {
    return cost(ADULT_PRICE, counts.adults())
        .add(cost(CHILD_PRICE, counts.children()))
        .add(cost(INFANT_PRICE, counts.infants()));
  }

  private static BigDecimal cost(final BigDecimal price, final int count) {
    return price.multiply(BigDecimal.valueOf(count));
  }
}
