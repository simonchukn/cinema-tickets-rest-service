package uk.gov.dwp.engineering.recruitment;

import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.purchase.PurchaseValidator;
import uk.gov.dwp.engineering.recruitment.purchase.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;

  private final PurchaseValidator purchaseValidator;

  private final TicketPriceCalculator ticketPriceCalculator;

  public CinemaTicketsServiceImpl(PaymentService paymentService,
      SeatReservationService seatReservationService,
      PurchaseValidator purchaseValidator,
      TicketPriceCalculator ticketPriceCalculator) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
    this.purchaseValidator = purchaseValidator;
    this.ticketPriceCalculator = ticketPriceCalculator;
  }

  @Override
  public BookingConfirmation purchaseTickets(final Long accountId,
      final TicketRequest... ticketRequests)
      throws InvalidBookingException {

    throw new UnsupportedOperationException("Not implemented yet");
  }
}
