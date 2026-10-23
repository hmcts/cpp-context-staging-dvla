package uk.gov.moj.cpp.stagingdvla.notifygateway;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.stagingdvla.azure.AzureResourceResolver;
import uk.gov.moj.cpp.stagingdvla.service.MalformedResultMessageException;
import uk.gov.moj.cpp.stagingdvla.service.NotifyGatewayResultService;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotifyGatewayResultConsumerTest {

    @Mock
    private AzureResourceResolver azureResourceResolver;

    @Mock
    private NotifyGatewayServiceBusConfiguration configuration;

    @Mock
    private NotifyGatewayResultService notifyGatewayResultService;

    @Mock
    private ServiceBusProcessorClient client;

    @InjectMocks
    private NotifyGatewayResultConsumer consumer;

    @Test
    public void shouldDelegateClientBuildingToTheSharedAzureResourceResolverAndStartIt() {
        when(azureResourceResolver.serviceBusProcessorClient(eq(configuration), any(), any())).thenReturn(client);

        consumer.init();

        verify(client).start();
    }

    @Test
    public void shouldCloseTheClientOnCleanup() {
        when(azureResourceResolver.serviceBusProcessorClient(eq(configuration), any(), any())).thenReturn(client);
        consumer.init();

        consumer.cleanup();

        verify(client).close();
    }

    @Test
    public void shouldNotFailCleanupWhenNeverInitialised() {
        // a fresh instance, not the @InjectMocks-managed one - @InjectMocks pre-populates the
        // client field with the mock by type regardless of init() having run, which would make
        // this case untestable against the shared `consumer`
        new NotifyGatewayResultConsumer().cleanup();
    }

    @Test
    public void shouldCompleteOnSuccessfulHandling() {
        final ServiceBusReceivedMessageContext context = mock(ServiceBusReceivedMessageContext.class);
        final ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(context.getMessage()).thenReturn(message);

        consumer.processMessage(context);

        verify(notifyGatewayResultService).handle(message);
        verify(context).complete();
        verify(context, never()).deadLetter();
    }

    @Test
    public void shouldDeadLetterOnMalformedResultMessage() {
        final ServiceBusReceivedMessageContext context = mock(ServiceBusReceivedMessageContext.class);
        final ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(context.getMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new MalformedResultMessageException("unrecognised subject"))
                .when(notifyGatewayResultService).handle(message);

        consumer.processMessage(context);

        verify(context).deadLetter();
        verify(context, never()).complete();
    }

    @Test
    public void shouldLeaveMessageUncompletedOnAnyOtherFailureSoServiceBusRedelivers() {
        final ServiceBusReceivedMessageContext context = mock(ServiceBusReceivedMessageContext.class);
        final ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(context.getMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new RuntimeException("transient failure recording the outcome"))
                .when(notifyGatewayResultService).handle(message);

        assertThrows(RuntimeException.class, () -> consumer.processMessage(context));

        verify(context, never()).complete();
        verify(context, never()).deadLetter();
    }

    @Test
    public void shouldLogAndNotThrowOnTransportError() {
        final ServiceBusErrorContext errorContext = mock(ServiceBusErrorContext.class);
        when(errorContext.getException()).thenReturn(new RuntimeException("transport error"));
        when(errorContext.getEntityPath()).thenReturn("stagingdvla-notification-result");

        consumer.processError(errorContext);

        verifyNoInteractions(notifyGatewayResultService);
    }
}
