package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationNotifyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationNotifyService.class);

    // clientContext is echoed back by notificationnotify on notification-sent / notification-failed;
    // stagingdvla prefixes its own value so those events can be matched back to the originating material
    public static final String CLIENT_CONTEXT = "clientContext";
    public static final String CLIENT_CONTEXT_PREFIX = "STAGINGDVLA_";

    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";

    // Shared by both result-event consumers (NotificationNotifyEventProcessor - legacy topic,
    // NotifyGatewayResultService - gateway reply queue, DVLAFS-19): a result payload belongs to a
    // DVLA document delivery only if its clientContext carries this prefix, and the rest of the
    // clientContext is that delivery's materialId.
    public static UUID materialIdFrom(final JsonObject payload) {
        return fromString(payload.getString(CLIENT_CONTEXT).substring(CLIENT_CONTEXT_PREFIX.length()));
    }

    public static boolean isStagingDvlaNotification(final JsonObject payload) {
        return payload.getString(CLIENT_CONTEXT, "").startsWith(CLIENT_CONTEXT_PREFIX);
    }

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    public void sendEmailNotification(final JsonEnvelope event, final JsonObject emailNotification) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending email notification for {} ", event.toObfuscatedDebugString());
        }

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(emailNotification)
                .withName(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE)
                .withMetadataFrom(event);
        sender.sendAsAdmin(jsonObjectEnvelope);
    }
}
