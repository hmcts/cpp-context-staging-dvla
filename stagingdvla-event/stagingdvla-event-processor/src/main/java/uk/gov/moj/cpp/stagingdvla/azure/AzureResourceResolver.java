package uk.gov.moj.cpp.stagingdvla.azure;

import uk.gov.moj.cpp.stagingdvla.notifygateway.NotifyGatewayServiceBusConfiguration;

import java.util.function.Consumer;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

/**
 * Builds the Azure SDK clients this service needs, under one of two credential schemes. Two
 * implementations exist — {@link ManagedIdentityAzureResourceResolver} and
 * {@link ConnectionStringAzureResourceResolver} — selected once by
 * {@link AzureResourceClientResolverProducer} based on
 * {@link AzureEmulatorConfiguration#isLocalEmulatorEnabled()}. Scoped to the notify-gateway Service
 * Bus client only (DVLAFS-12/19) — the blob file-store path keeps its own separate, untouched
 * credential-selection logic in {@code AzureFileStoreBlobContainerClientProducer}.
 */
public interface AzureResourceResolver {

    ServiceBusSenderClient serviceBusSenderClient(NotifyGatewayServiceBusConfiguration configuration);

    /**
     * Builds the long-running processor client staging-dvla's own result-queue consumer
     * (DVLAFS-19) owns the start/stop lifecycle of — see {@code NotifyGatewayResultConsumer}.
     * Mirrors {@link #serviceBusSenderClient(NotifyGatewayServiceBusConfiguration)}'s credential
     * handling exactly, just against {@link NotifyGatewayServiceBusConfiguration#getReplyQueueName()}
     * instead of the send queue.
     */
    ServiceBusProcessorClient serviceBusProcessorClient(NotifyGatewayServiceBusConfiguration configuration,
                                                         Consumer<ServiceBusReceivedMessageContext> processMessage,
                                                         Consumer<ServiceBusErrorContext> processError);
}
