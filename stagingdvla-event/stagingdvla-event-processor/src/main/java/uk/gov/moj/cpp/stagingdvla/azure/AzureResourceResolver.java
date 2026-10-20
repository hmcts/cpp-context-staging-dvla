package uk.gov.moj.cpp.stagingdvla.azure;

import uk.gov.moj.cpp.stagingdvla.notifygateway.NotifyGatewayServiceBusConfiguration;

import com.azure.messaging.servicebus.ServiceBusSenderClient;

/**
 * Builds the Azure SDK clients this service needs, under one of two credential schemes. Two
 * implementations exist — {@link ManagedIdentityAzureResourceResolver} and
 * {@link ConnectionStringAzureResourceResolver} — selected once by
 * {@link AzureResourceClientResolverProducer} based on
 * {@link AzureEmulatorConfiguration#isLocalEmulatorEnabled()}. Scoped to the notify-gateway Service
 * Bus client only (DVLAFS-12) — the blob file-store path keeps its own separate, untouched
 * credential-selection logic in {@code AzureFileStoreBlobContainerClientProducer}.
 */
public interface AzureResourceResolver {

    ServiceBusSenderClient serviceBusSenderClient(NotifyGatewayServiceBusConfiguration configuration);
}
