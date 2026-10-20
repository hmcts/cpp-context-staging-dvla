package uk.gov.moj.stagingdvla.stubs;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonObject;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

/**
 * Stands in for {@code cp-gov-uk-notify-gateway}'s {@code ng-send-email} queue — DVLAFS-12's real
 * destination, not deployed in this IT environment. Reads off the actual Service Bus emulator
 * ({@code cpp-servicebus-emulator}, {@code --profile servicebus}, pre-provisioned from
 * {@code cpp-developers-docker}'s {@code servicebus-emulator/Config.json}) that WildFly is
 * configured to send to in this environment (see {@code standalone.xml}'s
 * {@code notifygateway.servicebus.*} entries) — a real Azure Service Bus client round-trip, not a
 * mock, mirroring {@link NotifyStub}'s role for the legacy path.
 */
public class NotifyGatewayStub {

    // the emulator's static "same machine" connection string (see cpp-developers-docker's
    // standalone.xml notifygateway.servicebus.connection-string) - SAS_KEY_VALUE is a fixed
    // placeholder the emulator always accepts for UseDevelopmentEmulator=true, not a real secret
    private static final String CONNECTION_STRING =
            "Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;"
                    + "SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    private static final String SEND_QUEUE_NAME = "ng-send-email";

    private static final ServiceBusReceiverClient RECEIVER_CLIENT = new ServiceBusClientBuilder()
            .connectionString(CONNECTION_STRING)
            .receiver()
            .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
            .queueName(SEND_QUEUE_NAME)
            .buildClient();

    /**
     * Polls {@code ng-send-email} for the {@code send-email-notification} command
     * {@code DriverNotifiedEventProcessor.handleSentEmailNotificationEvent} sends for this
     * {@code clientContext} (DVLAFS-12, blob-addressed material) — the gateway-path equivalent of
     * {@link NotifyStub#verifyEmailNotificationSent(String)}. Receives in {@code RECEIVE_AND_DELETE}
     * mode (not peek-lock): messages are consumed as found, matching the throwaway nature of an IT
     * assertion rather than a real consumer's delivery guarantees.
     */
    public static ServiceBusReceivedMessage verifyEmailNotificationSentToGateway(final String clientContext) {
        final AtomicReference<ServiceBusReceivedMessage> matched = new AtomicReference<>();
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final Optional<ServiceBusReceivedMessage> found = findMessageForClientContext(clientContext);
            found.ifPresent(matched::set);
            return found.isPresent();
        });
        return matched.get();
    }

    private static Optional<ServiceBusReceivedMessage> findMessageForClientContext(final String clientContext) {
        final List<ServiceBusReceivedMessage> messages = RECEIVER_CLIENT.receiveMessages(10, ofSeconds(2))
                .stream()
                .toList();
        return messages.stream()
                .filter(message -> clientContext.equals(bodyOf(message).getString("clientContext", null)))
                .findFirst();
    }

    public static JsonObject bodyOf(final ServiceBusReceivedMessage message) {
        return createReader(new StringReader(message.getBody().toString())).readObject();
    }
}
