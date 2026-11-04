package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery.email;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY;
import static uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService.isStagingDvlaNotification;
import static uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService.materialIdFrom;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus;

import java.io.StringReader;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The business logic half of DVLAFS-19 (the result-queue lifecycle/transport half is
 * {@code NotifyGatewayResultConsumer}): parses a {@code notification-sent}/{@code
 * notification-failed} message off staging-dvla's own dedicated result queue and records the
 * outcome via {@link DocumentDeliveryStatusService}, exactly as {@code
 * NotificationNotifyEventProcessor} already does for the legacy topic - same filter/extraction
 * logic (shared on {@link NotificationNotifyService}), different transport. Deliberately NOT the
 * {@code @Singleton @Startup} EJB itself, so this class stays a plain, container-free unit under
 * test.
 */
public class NotifyGatewayResultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyGatewayResultService.class);

    private static final String NOTIFICATION_SENT_SUBJECT = "notification-sent";
    private static final String NOTIFICATION_FAILED_SUBJECT = "notification-failed";

    @Inject
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    public void handle(final ServiceBusReceivedMessage message) {
        final String subject = message.getSubject();
        final DvlaDocumentDeliveryEmailStatus status = statusFor(subject);
        final JsonObject payload = bodyOf(message);

        // nothing else ever publishes to this queue, so this is a defensive guard, not an
        // expected branch - mirrors NotificationNotifyEventProcessor's own filter
        if (!isStagingDvlaNotification(payload)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("ignoring {} on the notify-gateway result queue with no staging-dvla clientContext", subject);
            }
            return;
        }

        final UUID materialId = materialIdFrom(payload);
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY)
                .build();

        documentDeliveryStatusService.record(metadata, email(materialId, status));
    }

    private static DvlaDocumentDeliveryEmailStatus statusFor(final String subject) {
        if (NOTIFICATION_SENT_SUBJECT.equals(subject)) {
            return DvlaDocumentDeliveryEmailStatus.SUCCESS;
        }
        if (NOTIFICATION_FAILED_SUBJECT.equals(subject)) {
            return DvlaDocumentDeliveryEmailStatus.FAILED;
        }
        throw new MalformedResultMessageException("unrecognised notify-gateway result subject: " + subject);
    }

    private static JsonObject bodyOf(final ServiceBusReceivedMessage message) {
        try (JsonReader reader = createReader(new StringReader(message.getBody().toString()))) {
            return reader.readObject();
        } catch (final RuntimeException e) {
            throw new MalformedResultMessageException("unparseable notify-gateway result body: " + e.getMessage());
        }
    }
}
