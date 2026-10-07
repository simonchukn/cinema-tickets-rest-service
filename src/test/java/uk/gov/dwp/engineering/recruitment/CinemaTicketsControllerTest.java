package uk.gov.dwp.engineering.recruitment;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.dwp.engineering.recruitment.exception.RestExceptionHandler;
import uk.gov.dwp.engineering.recruitment.purchase.PurchaseValidator;
import uk.gov.dwp.engineering.recruitment.purchase.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsControllerTest {

  @Mock
  private PaymentService paymentService;

  @Mock
  private SeatReservationService seatReservationService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    CinemaTicketsService service = new CinemaTicketsServiceImpl(paymentService,
        seatReservationService, new PurchaseValidator(), new TicketPriceCalculator());

    mockMvc = MockMvcBuilders.standaloneSetup(new CinemaTicketsController(service))
        .setControllerAdvice(new RestExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("AC12: a valid booking returns 201 with seats and total cost")
  void validBookingIsCreated() throws Exception {
    mockMvc.perform(book("""
            {"accountId": 1, "ticketRequests": [
              {"type": "ADULT", "ticketCount": 2},
              {"type": "CHILD", "ticketCount": 1},
              {"type": "INFANT", "ticketCount": 1}]}
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountId").value(1))
        .andExpect(jsonPath("$.seatCount").value(3))
        .andExpect(jsonPath("$.totalCost").value(69.48));
  }

  @ParameterizedTest(name = "{0} returns 400")
  @MethodSource("rejectedBookings")
  void rejectedBookingReturnsBadRequest(String rule, String body, String detail)
      throws Exception {
    mockMvc.perform(book(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(detail));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  static Stream<Arguments> rejectedBookings() {
    return Stream.of(
        arguments("AC3: account id 0",
            """
            {"accountId": 0, "ticketRequests": [{"type": "ADULT", "ticketCount": 1}]}
            """,
            "Account id must be greater than zero"),
        arguments("AC4: a negative quantity",
            """
            {"accountId": 1, "ticketRequests": [
              {"type": "ADULT", "ticketCount": 1}, {"type": "CHILD", "ticketCount": -1}]}
            """,
            "Ticket quantities cannot be negative"),
        arguments("AC5: no adult",
            """
            {"accountId": 1, "ticketRequests": [{"type": "CHILD", "ticketCount": 1}]}
            """,
            "At least one adult ticket is required"),
        arguments("AC6: more infants than adults",
            """
            {"accountId": 1, "ticketRequests": [
              {"type": "ADULT", "ticketCount": 1}, {"type": "INFANT", "ticketCount": 2}]}
            """,
            "Each infant needs an adult to sit with"),
        arguments("AC7: 26 tickets",
            """
            {"accountId": 1, "ticketRequests": [{"type": "ADULT", "ticketCount": 26}]}
            """,
            "No more than 25 tickets can be bought at once"),
        arguments("AC8: a missing ticket list",
            """
            {"accountId": 1}
            """,
            "Ticket requests are required")
    );
  }

  @ParameterizedTest(name = "AC12: {0} returns 400")
  @MethodSource("malformedBookings")
  void malformedBookingReturnsBadRequest(String problem, String body) throws Exception {
    mockMvc.perform(book(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  static Stream<Arguments> malformedBookings() {
    return Stream.of(
        arguments("an unknown ticket type",
            """
            {"accountId": 1, "ticketRequests": [{"type": "SENIOR", "ticketCount": 1}]}
            """),
        arguments("a missing body", ""),
        arguments("broken JSON", "{\"accountId\": 1,")
    );
  }

  private static RequestBuilder book(String body) {
    return post("/cinema/bookings")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }
}
