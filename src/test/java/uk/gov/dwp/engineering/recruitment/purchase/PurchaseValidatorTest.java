package uk.gov.dwp.engineering.recruitment.purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

class PurchaseValidatorTest {

  private static final Long ACCOUNT_ID = 1L;

  private final PurchaseValidator validator = new PurchaseValidator();

  @Test
  @DisplayName("AC8: adult tickets on their own are valid")
  void acceptsAdultsOnly() {
    TicketCounts counts = validator.validate(ACCOUNT_ID, new TicketRequest(ADULT, 3));

    assertEquals(new TicketCounts(3, 0, 0), counts);
  }

  @Test
  @DisplayName("AC6: as many infants as adults is valid")
  void acceptsOneInfantPerAdult() {
    TicketCounts counts = validator.validate(ACCOUNT_ID,
        new TicketRequest(ADULT, 2), new TicketRequest(INFANT, 2));

    assertEquals(new TicketCounts(2, 0, 2), counts);
  }

  @ParameterizedTest(name = "AC7: {0} infants with 20 adults is valid")
  @ValueSource(ints = {4, 5})
  void acceptsUpToTwentyFiveTicketsIncludingInfants(int infants) {
    TicketCounts counts = validator.validate(ACCOUNT_ID,
        new TicketRequest(ADULT, 20), new TicketRequest(INFANT, infants));

    assertEquals(new TicketCounts(20, 0, infants), counts);
  }

  @Test
  @DisplayName("A ticket line with zero quantity is valid and adds nothing")
  void acceptsZeroQuantityLine() {
    TicketCounts counts = validator.validate(ACCOUNT_ID,
        new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 0), new TicketRequest(INFANT, 0));

    assertEquals(new TicketCounts(2, 0, 0), counts);
  }

  @Test
  @DisplayName("The same ticket type twice is added together")
  void sumsDuplicateTicketTypes() {
    TicketCounts counts = validator.validate(ACCOUNT_ID,
        new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 1), new TicketRequest(ADULT, 3));

    assertEquals(new TicketCounts(5, 1, 0), counts);
  }

  @ParameterizedTest(name = "AC3: account id {0} is rejected")
  @ValueSource(longs = {0, -1})
  void rejectsAccountIdNotAboveZero(long accountId) {
    assertRejected(accountId, new TicketRequest(ADULT, 1));
  }

  @Test
  @DisplayName("AC3: a missing account id is rejected")
  void rejectsMissingAccountId() {
    assertRejected(null, new TicketRequest(ADULT, 1));
  }

  @Test
  @DisplayName("AC4: a negative quantity is rejected")
  void rejectsNegativeQuantity() {
    assertRejected(ACCOUNT_ID, new TicketRequest(ADULT, 2), new TicketRequest(CHILD, -1));
  }

  @Test
  @DisplayName("AC5: child tickets without an adult are rejected")
  void rejectsChildWithoutAdult() {
    assertRejected(ACCOUNT_ID, new TicketRequest(CHILD, 1));
  }

  @Test
  @DisplayName("AC5: a request with no tickets is rejected")
  void rejectsEmptyRequest() {
    assertRejected(ACCOUNT_ID);
  }

  @Test
  @DisplayName("AC6: more infants than adults is rejected")
  void rejectsMoreInfantsThanAdults() {
    assertRejected(ACCOUNT_ID, new TicketRequest(ADULT, 1), new TicketRequest(INFANT, 2));
  }

  @Test
  @DisplayName("AC7: 26 tickets including infants is rejected")
  void rejectsMoreThanTwentyFiveTickets() {
    assertRejected(ACCOUNT_ID, new TicketRequest(ADULT, 20), new TicketRequest(INFANT, 6));
  }

  @Test
  @DisplayName("AC7: splitting adults over two lines cannot get past the limit")
  void rejectsDuplicateLinesThatExceedTheLimit() {
    assertRejected(ACCOUNT_ID, new TicketRequest(ADULT, 20), new TicketRequest(ADULT, 6));
  }

  @Test
  @DisplayName("AC8: a missing ticket list is rejected")
  void rejectsMissingTicketList() {
    assertRejected(ACCOUNT_ID, (TicketRequest[]) null);
  }

  @Test
  @DisplayName("AC8: an empty ticket line is rejected")
  void rejectsNullTicketLine() {
    assertRejected(ACCOUNT_ID, new TicketRequest(ADULT, 1), null);
  }

  @Test
  @DisplayName("AC8: a ticket line with no type is rejected")
  void rejectsMissingTicketType() {
    assertRejected(ACCOUNT_ID, new TicketRequest(ADULT, 1), new TicketRequest(null, 1));
  }

  private void assertRejected(Long accountId, TicketRequest... ticketRequests) {
    assertThrows(InvalidBookingException.class,
        () -> validator.validate(accountId, ticketRequests));
  }
}
