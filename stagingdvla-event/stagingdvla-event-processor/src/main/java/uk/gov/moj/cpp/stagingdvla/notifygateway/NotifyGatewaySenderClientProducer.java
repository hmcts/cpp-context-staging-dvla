package uk.gov.moj.cpp.stagingdvla.notifygateway;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

/**
 * CDI producer for the {@link ServiceBusSenderClient} that sends onto
 * {@code cp-gov-uk-notify-gateway}'s {@code ng-send-email} queue (DVLAFS-12).
 *
 * <p>Credential scheme mirrors {@code AzureFileStoreBlobContainerClientProducer}: a connection
 * string for the local/IT Service Bus emulator (no Azure AD support), {@code DefaultAzureCredential}
 * (AKS Workload Identity / FIC) everywhere else — see
 * {@link NotifyGatewayServiceBusConfiguration}.
 *
 * <p><strong>Scope note:</strong> as with {@code BlobContainerClient}, the Azure SDK's client
 * classes are not proxyable by Weld, so the {@link Produces} method is {@link Dependent} rather
 * than {@code @ApplicationScoped}; one shared instance is built once in {@link #initialise()}.
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class NotifyGatewaySenderClientProducer {

    @Inject
    private NotifyGatewayServiceBusConfiguration configuration;

    private ServiceBusSenderClient senderClient;

    @PostConstruct
    public void initialise() {
        senderClient = buildSenderClient(configuration);
    }

    @Produces
    @Dependent
    public ServiceBusSenderClient notifyGatewaySenderClient() {
        return senderClient;
    }

    protected ServiceBusSenderClient buildSenderClient(final NotifyGatewayServiceBusConfiguration configuration) {
        if (configuration.isAzuriteEnabled()) {
            return new ServiceBusClientBuilder()
                    .connectionString(configuration.getConnectionString())
                    .sender()
                    .queueName(configuration.getSendQueueName())
                    .buildClient();
        }

        return new ServiceBusClientBuilder()
                .fullyQualifiedNamespace(configuration.getFullyQualifiedNamespace())
                .credential(new DefaultAzureCredentialBuilder().build())
                .sender()
                .queueName(configuration.getSendQueueName())
                .buildClient();
    }
}
