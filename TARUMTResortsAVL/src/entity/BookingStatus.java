package entity;

/**
 * BookingStatus.java
 * Entity class -- status enum for a booking request.
 *
 * Status flow, driven/changed by the Control layer:
 *   PENDING     -- registered/queued, no room assigned yet
 *   CONFIRMED   -- room assigned, waiting for guest check-in
 *   CHECKED_IN  -- guest has checked in
 *   CHECKED_OUT -- guest has checked out
 *   CANCELLED   -- this booking request was cancelled
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}
