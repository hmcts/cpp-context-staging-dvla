package uk.gov.moj.cpp.stagingdvla.service;

/**
 * A notify-gateway result message ({@code notification-sent}/{@code notification-failed} on
 * staging-dvla's own dedicated result queue, DVLAFS-19) that cannot be processed as-is: an
 * unparseable body, or a {@code subject} other than the two recognised event names. Distinct
 * from a transient failure (e.g. a downstream call failing inside
 * {@code DocumentDeliveryStatusService.record(...)}) - a transient failure should be retried by
 * leaving the message uncompleted; this one is poison and is dead-lettered instead, since retrying
 * it will never succeed.
 */
public class MalformedResultMessageException extends RuntimeException {

    public MalformedResultMessageException(final String message) {
        super(message);
    }
}
