package uk.gov.moj.cpp.stagingdvla.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery.email;
import static uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService.CLIENT_CONTEXT;
import static uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService.CLIENT_CONTEXT_PREFIX;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class NotificationNotifyEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationNotifyEventProcessor.class);

    private static final String NOTIFICATION_ID = "notificationId";

    @Inject
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    // Only ids/status are logged - the payload can carry recipient address and email body
    @Handles("public.notificationnotify.events.notification-sent")
    public void handleNotificationSent(final JsonEnvelope envelope) {
        recordEmailStatus(envelope, DvlaDocumentDeliveryEmailStatus.SUCCESS);
    }

    @Handles("public.notificationnotify.events.notification-failed")
    public void handleNotificationFailed(final JsonEnvelope envelope) {
        recordEmailStatus(envelope, DvlaDocumentDeliveryEmailStatus.FAILED);
    }

    // notificationnotify publishes these events for every context's emails; only the ones whose
    // clientContext carries the stagingdvla prefix (set on the blob path) belong to a DVLA document
    // delivery, and the rest of the clientContext is that delivery's materialId
    private void recordEmailStatus(final JsonEnvelope envelope, final DvlaDocumentDeliveryEmailStatus emailStatus) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (isStagingDvlaNotification(payload)) {
            documentDeliveryStatusService.record(envelope.metadata(), email(materialIdFrom(payload), emailStatus));
        }
    }

    private static UUID materialIdFrom(final JsonObject payload) {
        return fromString(payload.getString(CLIENT_CONTEXT).substring(CLIENT_CONTEXT_PREFIX.length()));
    }

    private static boolean isStagingDvlaNotification(final JsonObject payload) {
        return payload.getString(CLIENT_CONTEXT, "").startsWith(CLIENT_CONTEXT_PREFIX);
    }
}
