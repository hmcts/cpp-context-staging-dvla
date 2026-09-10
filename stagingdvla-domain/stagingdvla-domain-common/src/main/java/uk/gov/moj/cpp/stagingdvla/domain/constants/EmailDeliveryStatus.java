package uk.gov.moj.cpp.stagingdvla.domain.constants;

/**
 * Values for {@code dvla_document_delivery.email_status}.
 *
 * <p>{@link #SUCCESS} and {@link #FAILED} are terminal and are set from the notificationnotify
 * result events. {@link #PENDING} and {@link #NOT_REQUIRED} are set when the delivery row is
 * created, which is not yet implemented - nothing writes rows to {@code dvla_document_delivery}
 * today. Whether an email is required at all is decided by
 * {@code SystemDocGeneratorEventProcessor.shouldSendEmailNotification}.
 */
public enum EmailDeliveryStatus {

    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    NOT_REQUIRED("NOT_REQUIRED");

    private final String status;

    EmailDeliveryStatus(final String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Whether a currently stored value has already resolved and must not be overwritten.
     * Terminal states are sticky, which is what makes the projection idempotent under redelivery
     * and safe against out of order arrival.
     *
     * @param status the value currently held in the column, which may be null
     * @return true if the stored value is already SUCCESS or FAILED
     */
    public static boolean isTerminal(final String status) {
        return SUCCESS.getStatus().equals(status) || FAILED.getStatus().equals(status);
    }
}
