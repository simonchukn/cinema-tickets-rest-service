package uk.gov.dwp.engineering.recruitment.purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketCountsTest {

  @ParameterizedTest(name = "AC2: {0} adult, {1} child, {2} infant needs {3} seats")
  @CsvSource({
      "1, 0, 0, 1",
      "1, 1, 0, 2",
      "2, 1, 2, 3"
  })
  void infantsDoNotGetASeat(int adults, int children, int infants, long expectedSeats) {
    TicketCounts counts = new TicketCounts(adults, children, infants);

    assertEquals(expectedSeats, counts.seatCount());
  }
}
