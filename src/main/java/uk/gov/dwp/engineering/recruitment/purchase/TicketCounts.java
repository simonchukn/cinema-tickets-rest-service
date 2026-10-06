package uk.gov.dwp.engineering.recruitment.purchase;

public record TicketCounts(int adults, int children, int infants) {

  public long seatCount() {
    return (long) adults + children;
  }
}
