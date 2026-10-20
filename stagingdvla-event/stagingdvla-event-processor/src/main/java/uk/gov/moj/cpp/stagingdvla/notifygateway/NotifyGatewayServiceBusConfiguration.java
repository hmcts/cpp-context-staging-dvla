package uk.gov.moj.cpp.stagingdvla.notifygateway;

import uk.gov.justice.services.common.configuration.Value;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JNDI-backed configuration for staging-dvla's send side of the {@code cp-gov-uk-notify-gateway}
 * Azure Service Bus integration (DVLAFS-12). The local-emulator-vs-real-Azure switch lives in the
 * shared {@code uk.gov.moj.cpp.stagingdvla.azure.AzureEmulatorConfiguration}
 * (reads {@code azure.local-emulator-enabled}) — this class only carries values specific to this one
 * Service Bus integration: the emulator's connection string (a different value from Azurite's own,
 * hence still its own key here), the real namespace, and the queue names.
 *
 * <p>The queue names are supplied fully-resolved (the {@code {{ stack }}} prefix from
 * {@code cpp-aks-deploy}/{@code cpp-terraform-azurerm-servicebus} is expanded at deploy time into
 * the pod's env — this class never does its own stack-name templating).
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class NotifyGatewayServiceBusConfiguration {

    @Inject
    @Value(key = "notifygateway.servicebus.connection-string", defaultValue = "")
    private String connectionString;

    @Inject
    @Value(key = "notifygateway.servicebus.fully-qualified-namespace", defaultValue = "")
    private String fullyQualifiedNamespace;

    @Inject
    @Value(key = "notifygateway.servicebus.send-queue-name")
    private String sendQueueName;

    @Inject
    @Value(key = "notifygateway.servicebus.reply-queue-name")
    private String replyQueueName;

    /** Only meaningful when {@code AzureEmulatorConfiguration.isLocalEmulatorEnabled()} is {@code true}. */
    public String getConnectionString() {
        return connectionString;
    }

    /** The {@code sb<env>ccm01} namespace host, e.g. {@code sbsteccm01.servicebus.windows.net}. Only used when {@code AzureEmulatorConfiguration.isLocalEmulatorEnabled()} is {@code false}. */
    public String getFullyQualifiedNamespace() {
        return fullyQualifiedNamespace;
    }

    /** The gateway's shared command queue, resolved, e.g. {@code steccm01.ng-send-email}. */
    public String getSendQueueName() {
        return sendQueueName;
    }

    /** staging-dvla's own dedicated result queue (DVLAFS-18), resolved, e.g. {@code steccm01.stagingdvla-notification-result}. Set as the outbound message's {@code ReplyTo}. */
    public String getReplyQueueName() {
        return replyQueueName;
    }
}
