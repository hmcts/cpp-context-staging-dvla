package uk.gov.moj.cpp.stagingdvla.domain.constants;

/**
 * The {@code clientContext} value staging dvla sets on
 * {@code notificationnotify.send-email-notification}, and matches on when
 * {@code public.notificationnotify.events.notification-sent} / {@code ...notification-failed} come
 * back.
 *
 * <p>{@code clientContext} is an existing optional string on the notificationnotify command which
 * that context echoes onto both of its public result events. It identifies the owning context and
 * nothing more: every notification event published anywhere on the platform is delivered to every
 * subscriber, so a subscriber needs a way to recognise its own. Other contexts do the same with
 * their own literal - correspondence sets {@code "correspondence"}.
 *
 * <p>There is no framework method that returns the current context name; the only
 * {@code ContextNameProvider} in the estate is an integration-test helper in progression. A static
 * constant is the established convention.
 *
 * <p>Per-request correlation does <em>not</em> travel in this field. It travels in
 * {@code notificationId}, which is caller-chosen and required on both result events - see
 * {@code DriverNotifiedEventProcessor}, which sets it to the request's {@code materialId}.
 */
public final class StagingDvlaClientContext {

    /** The value staging dvla stamps on every email notification it sends. */
    public static final String CLIENT_CONTEXT = "STAGING_DVLA";

    private StagingDvlaClientContext() {
    }

    /**
     * Whether a client context read off an inbound notification event was written by this context.
     *
     * @param clientContext the raw value, which may be null - the field is optional on both events,
     *                      and notificationnotify's markAsInvalid route omits it entirely
     * @return true if this context sent the notification the event relates to
     */
    public static boolean isOurs(final String clientContext) {
        return CLIENT_CONTEXT.equals(clientContext);
    }
}
