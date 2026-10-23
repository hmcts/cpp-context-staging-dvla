package uk.gov.moj.stagingdvla.stubs;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonObject;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

/**
 * Stands in for {@code cp-gov-uk-notify-gateway}'s {@code ng-send-email} queue — DVLAFS-12's real
 * destination, not deployed in this IT environment. Reads off the actual Service Bus emulator
 * ({@code cpp-servicebus-emulator}, {@code --profile servicebus}, pre-provisioned from
 * {@code cpp-developers-docker}'s {@code servicebus-emulator/Config.json}) that WildFly is
 * configured to send to in this environment (see {@code standalone.xml}'s
 * {@code notifygateway.servicebus.*} entries) — a real Azure Service Bus client round-trip, not a
 * mock, mirroring {@link NotifyStub}'s role for the legacy path.
 *
 * <p>Also stands in for the gateway's own <em>reply</em>, DVLAFS-19: {@link #publishNotificationSent}
 * and {@link #publishNotificationFailed} publish onto staging-dvla's own dedicated result queue
 * ({@code stagingdvla-notification-result}), the queue {@code NotifyGatewayResultConsumer} (the
 * real consumer under test) listens on — the inverse role to {@link #verifyEmailNotificationSentToGateway},
 * which only ever receives.
 */
public class NotifyGatewayStub {

    // the emulator's static "same machine" endpoint (see cpp-developers-docker's standalone.xml
    // notifygateway.servicebus.connection-string) - SAS_KEY_VALUE is a fixed placeholder the
    // emulator always accepts for UseDevelopmentEmulator=true, not a real secret.
    // Endpoint overridable via SERVICEBUS_EMULATOR_ENDPOINT: in the mdv validation pipeline the
    // emulator isn't reachable at localhost (it's a separate in-cluster pod, not port-forwarded
    // like cpp-developers-docker's socat sidecar), so validation-tests.yaml injects the real
    // in-cluster FQDN there - mirrors AZURITE_ENDPOINT's same role for NotifyStub/MaterialStub.
    private static final String CONNECTION_STRING = "Endpoint="
            + Optional.ofNullable(System.getenv("SERVICEBUS_EMULATOR_ENDPOINT")).orElse("sb://localhost")
            + ";SharedAccessKeyName=RootManageSharedAccessKey;"
            + "SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    private static final String SEND_QUEUE_NAME = "ng-send-email";
    // Literal, not stack-prefixed - confirmed the IT-environment expectation via
    // DvlaNotificationIT's own ReplyTo assertions (`"stagingdvla-notification-result"`).
    private static final String RESULT_QUEUE_NAME = "stagingdvla-notification-result";

    private static final String NOTIFICATION_SENT_SUBJECT = "notification-sent";
    private static final String NOTIFICATION_FAILED_SUBJECT = "notification-failed";

    private static final ServiceBusReceiverClient RECEIVER_CLIENT = new ServiceBusClientBuilder()
            .connectionString(CONNECTION_STRING)
            .receiver()
            .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
            .queueName(SEND_QUEUE_NAME)
            .buildClient();

    private static final ServiceBusSenderClient RESULT_SENDER_CLIENT = new ServiceBusClientBuilder()
            .connectionString(CONNECTION_STRING)
            .sender()
            .queueName(RESULT_QUEUE_NAME)
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

    /**
     * Publishes a {@code notification-sent} result onto staging-dvla's own dedicated result queue
     * — simulates the gateway's reply for a successful send (DVLAFS-19). Body matches the
     * golden-master {@code notification-sent} schema's required fields plus {@code clientContext}
     * (the join key {@code NotifyGatewayResultConsumer} needs) and {@code sendToAddress} (realistic
     * optional field, not load-bearing for the assertions this stub supports).
     */
    public static void publishNotificationSent(final String clientContext, final UUID notificationId) {
        final JsonObject body = createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("sentTime", Instant.now().toString())
                .add("sendToAddress", "driver@example.test")
                .add("clientContext", clientContext)
                .build();
        send(NOTIFICATION_SENT_SUBJECT, body);
    }

    /**
     * Publishes a {@code notification-failed} result onto staging-dvla's own dedicated result
     * queue — simulates the gateway's reply for a failed send (DVLAFS-19).
     */
    public static void publishNotificationFailed(final String clientContext, final UUID notificationId,
                                                  final String errorMessage) {
        final JsonObject body = createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("failedTime", Instant.now().toString())
                .add("errorMessage", errorMessage)
                .add("clientContext", clientContext)
                .build();
        send(NOTIFICATION_FAILED_SUBJECT, body);
    }

    private static void send(final String subject, final JsonObject body) {
        final ServiceBusMessage message = new ServiceBusMessage(body.toString());
        message.setSubject(subject);
        RESULT_SENDER_CLIENT.sendMessage(message);
    }
}
