package uk.gov.moj.cpp.stagingdvla.notifygateway;

import uk.gov.moj.cpp.stagingdvla.azure.AzureResourceResolver;
import uk.gov.moj.cpp.stagingdvla.service.MalformedResultMessageException;
import uk.gov.moj.cpp.stagingdvla.service.NotifyGatewayResultService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the start/stop lifecycle of the {@link ServiceBusProcessorClient} that consumes staging-dvla's
 * own dedicated result queue (DVLAFS-19, {@code NotifyGatewayServiceBusConfiguration#getReplyQueueName()}).
 * Shaped directly on the only other {@code @Singleton @Startup} precedent in this module,
 * {@code uk.gov.moj.cpp.stagingdvla.service.scheduler.NotifyDrivingConvictionRetryScheduler} -
 * see {@code adrs/004-notify-gateway-result-consumer-transport.md} in the CR's KB workspace for
 * the full decision record (not accessible from this repo; summarised here only).
 *
 * <p>The actual parsing/recording logic lives in {@link NotifyGatewayResultService}, kept
 * deliberately separate so it stays unit-testable without a container - this class is only the
 * transport lifecycle and the complete/dead-letter decision per message.
 */
@Singleton
@Startup
public class NotifyGatewayResultConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyGatewayResultConsumer.class);

    @Inject
    private AzureResourceResolver azureResourceResolver;

    @Inject
    private NotifyGatewayServiceBusConfiguration configuration;

    @Inject
    private NotifyGatewayResultService notifyGatewayResultService;

    private ServiceBusProcessorClient client;

    @PostConstruct
    void init() {
        client = azureResourceResolver.serviceBusProcessorClient(configuration, this::processMessage, this::processError);
        client.start();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NotifyGatewayResultConsumer started on {}", configuration.getReplyQueueName());
        }
    }

    @PreDestroy
    void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    void processMessage(final ServiceBusReceivedMessageContext context) {
        try {
            notifyGatewayResultService.handle(context.getMessage());
            context.complete();
        } catch (final MalformedResultMessageException e) {
            LOGGER.warn("dead-lettering unprocessable notify-gateway result message", e);
            context.deadLetter();
        }
        // any other exception (e.g. a transient failure recording the outcome) propagates and is
        // left uncompleted - Service Bus redelivers per the queue's maxDeliveryCount, which is
        // intentional: the consumer must tolerate redelivery/duplicates (DVLAFS-19 AC-5), not
        // suppress it as an error
    }

    void processError(final ServiceBusErrorContext context) {
        LOGGER.error("notify-gateway result consumer error on entity {}", context.getEntityPath(), context.getException());
    }
}
