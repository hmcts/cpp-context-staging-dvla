package uk.gov.moj.cpp.stagingdvla.notifygateway;

import uk.gov.justice.services.common.configuration.Value;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JNDI-backed configuration for staging-dvla's send side of the {@code cp-gov-uk-notify-gateway}
 * Azure Service Bus integration (DVLAFS-12). Reuses {@code azure.filestore.azurite-enabled} —
 * {@code AzureFileStoreBlobConfiguration}'s own local/real-environment switch — as one emulator-mode
 * toggle for every Azure client this service builds, rather than a second, independently-settable
 * flag: {@code "true"} (local dev/IT only) makes {@link NotifyGatewaySenderClientProducer} use
 * {@code notifygateway.servicebus.connection-string} directly (the Service Bus emulator has no
 * Azure AD support, so it needs a connection string — a different value from Azurite's own, hence
 * still its own key); {@code "false"} (the default, every real environment) makes it authenticate
 * via {@code DefaultAzureCredential} (Workload Identity on AKS) against
 * {@code notifygateway.servicebus.fully-qualified-namespace}.
 *
 * <p>The queue names are supplied fully-resolved (the {@code {{ stack }}} prefix from
 * {@code cpp-aks-deploy}/{@code cpp-terraform-azurerm-servicebus} is expanded at deploy time into
 * the pod's env — this class never does its own stack-name templating).
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class NotifyGatewayServiceBusConfiguration {

    @Inject
    @Value(key = "azure.filestore.azurite-enabled", defaultValue = "false")
    private String azuriteEnabled;

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

    /**
     * {@code true} when {@code azure.filestore.azurite-enabled} is explicitly set to {@code "true"}
     * (local dev/IT — the same flag {@code AzureFileStoreBlobConfiguration} reads), in which case
     * {@link NotifyGatewaySenderClientProducer} uses {@link #getConnectionString()} directly;
     * {@code false} (the default, every real environment) means it authenticates via
     * {@code DefaultAzureCredential}.
     */
    public boolean isAzuriteEnabled() {
        return Boolean.parseBoolean(azuriteEnabled);
    }

    /** Only meaningful when {@link #isAzuriteEnabled()} is {@code true}. */
    public String getConnectionString() {
        return connectionString;
    }

    /** The {@code sb<env>ccm01} namespace host, e.g. {@code sbsteccm01.servicebus.windows.net}. Only used when {@link #isAzuriteEnabled()} is {@code false}. */
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
