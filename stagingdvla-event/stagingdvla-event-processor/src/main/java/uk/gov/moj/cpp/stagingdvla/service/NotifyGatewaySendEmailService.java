package uk.gov.moj.cpp.stagingdvla.service;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.notifygateway.NotifyGatewayServiceBusConfiguration;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the D20 email request onto {@code cp-gov-uk-notify-gateway}'s {@code ng-send-email} queue
 * (DVLAFS-12) — the flag-open replacement for {@link NotificationNotifyService}'s legacy command.
 *
 * <p>The gateway's inbound contract is a raw ASB message, not the JEE framework's own envelope
 * shape: a flat JSON body matching {@code command-send-email-notification.schema.json}, the ASB
 * {@code subject} set to the literal command name (the gateway asserts this), {@code correlationId}
 * in the application properties, and the native ASB {@code ReplyTo} property naming staging-dvla's
 * own dedicated result queue (DVLAFS-18) — not a body field.
 */
public class NotifyGatewaySendEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyGatewaySendEmailService.class);

    private static final String SEND_EMAIL_NOTIFICATION_SUBJECT = "send-email-notification";
    private static final String CORRELATION_ID = "correlationId";

    @Inject
    private ServiceBusSenderClient notifyGatewaySenderClient;

    @Inject
    private NotifyGatewayServiceBusConfiguration configuration;

    public void sendEmailNotification(final JsonEnvelope event, final JsonObject emailNotification) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending email notification to the notify gateway for {} ", event.toObfuscatedDebugString());
        }

        final ServiceBusMessage message = new ServiceBusMessage(emailNotification.toString().getBytes(StandardCharsets.UTF_8));
        message.setSubject(SEND_EMAIL_NOTIFICATION_SUBJECT);
        message.setReplyTo(configuration.getReplyQueueName());
        message.getApplicationProperties().put(CORRELATION_ID, correlationIdOf(event));

        notifyGatewaySenderClient.sendMessage(message);
    }

    // the gateway's own contract says "missing correlationId -> generate"; the envelope's own id
    // is a stable, already-correlatable value to generate from, rather than a fresh random one
    private String correlationIdOf(final JsonEnvelope event) {
        return event.metadata().clientCorrelationId().orElseGet(() -> event.metadata().id().toString());
    }
}
