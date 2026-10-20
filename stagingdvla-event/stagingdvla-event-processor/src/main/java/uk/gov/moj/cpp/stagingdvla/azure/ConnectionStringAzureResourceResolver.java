package uk.gov.moj.cpp.stagingdvla.azure;

import uk.gov.moj.cpp.stagingdvla.notifygateway.NotifyGatewayServiceBusConfiguration;

import javax.enterprise.inject.Vetoed;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

/**
 * Connection-string resolution for local dev/IT Azure emulators (the Service Bus emulator) —
 * these have no Azure AD support, so they need this instead of the
 * {@link ManagedIdentityAzureResourceResolver} path every real environment uses.
 *
 * <p>{@code @Vetoed}: only {@link AzureResourceClientResolverProducer} ever constructs this — see
 * {@link ManagedIdentityAzureResourceResolver} for why this matters even with an implicit no-arg
 * constructor.
 */
@Vetoed
public final class ConnectionStringAzureResourceResolver implements AzureResourceResolver {

    @Override
    public ServiceBusSenderClient serviceBusSenderClient(final NotifyGatewayServiceBusConfiguration configuration) {
        return new ServiceBusClientBuilder()
                .connectionString(configuration.getConnectionString())
                .sender()
                .queueName(configuration.getSendQueueName())
                .buildClient();
    }
}
