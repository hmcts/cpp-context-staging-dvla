package uk.gov.moj.cpp.stagingdvla.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.cpp.stagingdvla.command.RecordEmailDeliveryOutcome;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.domain.constants.StagingDvlaClientContext;
import uk.gov.moj.cpp.stagingdvla.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes the email delivery outcome that notificationnotify already publishes but staging dvla
 * has never subscribed to, and turns it into a command on this context's own stream.
 *
 * <p>The D20 email send is fire and forget today: {@code DriverNotifiedEventProcessor} sends
 * {@code notificationnotify.send-email-notification} and never learns whether the email arrived.
 * These two events close that loop.
 *
 * <p>Two fields do the routing, and they do different jobs. {@code clientContext} says which
 * context owns the notification - every notification event published anywhere on the platform is
 * delivered to this subscription, so the great majority of what arrives belongs to another context
 * and is discarded on an equality check before anything else happens. {@code notificationId} then
 * resolves to the materialId through {@link SystemIdMapperService}, using the mapping recorded when
 * the email was sent. The notification id stays unique per notification, because a material can be
 * notified more than once.
 */
@ServiceComponent(EVENT_PROCESSOR)
public class NotificationNotifyEventProcessor {

    public static final String NOTIFICATION_SENT_EVENT = "public.notificationnotify.events.notification-sent";
    public static final String NOTIFICATION_FAILED_EVENT = "public.notificationnotify.events.notification-failed";
    public static final String STAGINGDVLA_COMMAND_RECORD_EMAIL_DELIVERY_OUTCOME = "stagingdvla.command.record-email-delivery-outcome";

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationNotifyEventProcessor.class.getCanonicalName());

    private static final String FIELD_CLIENT_CONTEXT = "clientContext";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    private static final String FIELD_SENT_TIME = "sentTime";
    private static final String FIELD_FAILED_TIME = "failedTime";
    private static final String FIELD_ERROR_MESSAGE = "errorMessage";
    private static final String FIELD_STATUS_CODE = "statusCode";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Handles(NOTIFICATION_SENT_EVENT)
    public void handleNotificationSent(final JsonEnvelope event) {
        recordOutcome(event, true, FIELD_SENT_TIME);
    }

    @Handles(NOTIFICATION_FAILED_EVENT)
    public void handleNotificationFailed(final JsonEnvelope event) {
        recordOutcome(event, false, FIELD_FAILED_TIME);
    }

    private void recordOutcome(final JsonEnvelope event, final boolean delivered, final String timeField) {

        final JsonObject payload = event.payloadAsJsonObject();
        final String eventName = event.metadata().name();

        // clientContext is optional on both events, and notificationnotify's markAsInvalid route
        // omits it entirely, so absence is expected rather than exceptional.
        final String clientContext = payload.getString(FIELD_CLIENT_CONTEXT, null);

        if (!StagingDvlaClientContext.isOurs(clientContext)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignoring {} - clientContext '{}' does not belong to staging dvla", eventName, clientContext);
            }
            return;
        }

        // notificationId is required on both events. It is unique per notification, so the
        // materialId behind it comes from the mapping recorded at send time.
        final UUID notificationId = uuidOrNull(payload, FIELD_NOTIFICATION_ID);
        if (notificationId == null) {
            LOGGER.warn("Discarding {} - it is staging dvla's but carries no usable notificationId to correlate on",
                    eventName);
            return;
        }

        final UUID materialId = systemIdMapperService.getMaterialIdForNotificationId(notificationId.toString())
                .map(SystemIdMapping::getTargetId)
                .orElse(null);

        if (materialId == null) {
            LOGGER.warn("Discarding {} - no materialId is mapped to notificationId {}", eventName, notificationId);
            return;
        }

        final RecordEmailDeliveryOutcome outcome = RecordEmailDeliveryOutcome.recordEmailDeliveryOutcome()
                .withMaterialId(materialId)
                .withDelivered(delivered)
                .withNotificationId(notificationId)
                .withOccurredAt(timestampOrNull(payload, timeField))
                .withErrorMessage(payload.getString(FIELD_ERROR_MESSAGE, null))
                .withStatusCode(payload.containsKey(FIELD_STATUS_CODE) ? payload.getInt(FIELD_STATUS_CODE) : null)
                .build();

        LOGGER.info("Recording email delivery outcome for materialId {}: delivered={}", materialId, delivered);

        // Envelope.envelopeFrom rather than the static Enveloper.envelop, which needs a live CDI
        // container and would make this handler untestable outside one.
        sender.send(Envelope.envelopeFrom(
                metadataFrom(event.metadata()).withName(STAGINGDVLA_COMMAND_RECORD_EMAIL_DELIVERY_OUTCOME).build(),
                objectToJsonObjectConverter.convert(outcome)));
    }

    private UUID uuidOrNull(final JsonObject payload, final String field) {
        final String value = payload.getString(field, null);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException notAUuid) {
            LOGGER.warn("Ignoring unparseable {} '{}' on notification event", field, value);
            return null;
        }
    }

    private ZonedDateTime timestampOrNull(final JsonObject payload, final String field) {
        final String value = payload.getString(field, null);
        if (value == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value);
        } catch (final DateTimeParseException notATimestamp) {
            LOGGER.warn("Ignoring unparseable {} '{}' on notification event", field, value);
            return null;
        }
    }
}
