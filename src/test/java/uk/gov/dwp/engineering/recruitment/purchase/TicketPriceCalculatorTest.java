package uk.gov.dwp.engineering.recruitment.purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketPriceCalculatorTest {

  private final TicketPriceCalculator calculator = new TicketPriceCalculator();

  @ParameterizedTest(name = "AC1: {0} adult, {1} child, {2} infant costs {3}")
  @CsvSource({
      "1, 0, 0, 25.99",
      "0, 1, 0, 17.50",
      "0, 0, 1, 0.00",
      "2, 1, 1, 69.48"
  })
  void calculatesTotalPrice(int adults, int children, int infants, String expected) {
    TicketCounts counts = new TicketCounts(adults, children, infants);

    assertEquals(new BigDecimal(expected), calculator.totalPrice(counts));
  }
}
