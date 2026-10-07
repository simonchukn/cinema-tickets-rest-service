package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.purchase.PurchaseValidator;
import uk.gov.dwp.engineering.recruitment.purchase.TicketCounts;
import uk.gov.dwp.engineering.recruitment.purchase.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;

  private final PurchaseValidator purchaseValidator;

  private final TicketPriceCalculator ticketPriceCalculator;

  public CinemaTicketsServiceImpl(final PaymentService paymentService,
      final SeatReservationService seatReservationService,
      final PurchaseValidator purchaseValidator,
      final TicketPriceCalculator ticketPriceCalculator) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
    this.purchaseValidator = purchaseValidator;
    this.ticketPriceCalculator = ticketPriceCalculator;
  }

  @Override
  public BookingConfirmation purchaseTickets(final Long accountId,
      final TicketRequest... ticketRequests)
      throws InvalidBookingException {

    final TicketCounts counts = purchaseValidator.validate(accountId, ticketRequests);
    final BigDecimal totalCost = ticketPriceCalculator.totalCost(counts);
    final long seatCount = counts.seatCount();

    paymentService.debitAccount(accountId, totalCost);
    seatReservationService.reserveSeats(accountId, seatCount);

    return new BookingConfirmation(accountId, seatCount, totalCost);
  }
}
