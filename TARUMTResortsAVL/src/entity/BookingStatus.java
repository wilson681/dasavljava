package entity;
/*
 * Represents the status of a booking.
 *
 * Status flow:
 * PENDING -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT
 * A booking can also be CANCELLED.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}
