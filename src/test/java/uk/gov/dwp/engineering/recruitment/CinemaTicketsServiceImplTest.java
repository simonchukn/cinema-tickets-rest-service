package uk.gov.dwp.engineering.recruitment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.purchase.PurchaseValidator;
import uk.gov.dwp.engineering.recruitment.purchase.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

  private static final Long ACCOUNT_ID = 1L;

  // 2 adults, 1 child and 1 infant: 3 seats costing 69.48
  private static final TicketRequest[] FAMILY = {
      new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 1), new TicketRequest(INFANT, 1)
  };

  @Mock
  private PaymentService paymentService;

  @Mock
  private SeatReservationService seatReservationService;

  private CinemaTicketsService service;

  @BeforeEach
  void setUp() {
    service = new CinemaTicketsServiceImpl(paymentService, seatReservationService,
        new PurchaseValidator(), new TicketPriceCalculator());
  }

  @Test
  @DisplayName("AC9: a valid request pays the total price from the right account")
  void paysTheTotalPrice() {
    service.purchaseTickets(ACCOUNT_ID, FAMILY);

    ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
    verify(paymentService).debitAccount(eq(ACCOUNT_ID), amount.capture());
    assertEquals(new BigDecimal("69.48"), amount.getValue());
  }

  @Test
  @DisplayName("AC10: a valid request reserves seats for adults and children only")
  void reservesSeatsForAdultsAndChildren() {
    service.purchaseTickets(ACCOUNT_ID, FAMILY);

    ArgumentCaptor<Long> seats = ArgumentCaptor.forClass(Long.class);
    verify(seatReservationService).reserveSeats(eq(ACCOUNT_ID), seats.capture());
    assertEquals(3L, seats.getValue());
  }

  @Test
  @DisplayName("AC10: payment is taken before seats are reserved")
  void paysBeforeReserving() {
    service.purchaseTickets(ACCOUNT_ID, FAMILY);

    InOrder order = inOrder(paymentService, seatReservationService);
    order.verify(paymentService).debitAccount(any(), any());
    order.verify(seatReservationService).reserveSeats(any(), anyLong());
  }

  @Test
  @DisplayName("AC12: the confirmation shows the account, seats and total cost")
  void returnsConfirmation() {
    BookingConfirmation confirmation = service.purchaseTickets(ACCOUNT_ID, FAMILY);

    assertEquals(new BookingConfirmation(ACCOUNT_ID, 3L, new BigDecimal("69.48")), confirmation);
  }

  @ParameterizedTest(name = "AC11: {0} makes no payment or reservation")
  @MethodSource("invalidRequests")
  void rejectedRequestTouchesNeitherService(String reason, Long accountId,
      TicketRequest[] ticketRequests) {
    assertThrows(InvalidBookingException.class,
        () -> service.purchaseTickets(accountId, ticketRequests));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  static Stream<Arguments> invalidRequests() {
    return Stream.of(
        arguments("account id 0", 0L, new TicketRequest[] {new TicketRequest(ADULT, 1)}),
        arguments("a negative quantity", ACCOUNT_ID,
            new TicketRequest[] {new TicketRequest(ADULT, 1), new TicketRequest(CHILD, -1)}),
        arguments("no adult", ACCOUNT_ID, new TicketRequest[] {new TicketRequest(CHILD, 1)}),
        arguments("more infants than adults", ACCOUNT_ID,
            new TicketRequest[] {new TicketRequest(ADULT, 1), new TicketRequest(INFANT, 2)}),
        arguments("26 tickets", ACCOUNT_ID, new TicketRequest[] {new TicketRequest(ADULT, 26)}),
        arguments("a missing ticket list", ACCOUNT_ID, null)
    );
  }
}
