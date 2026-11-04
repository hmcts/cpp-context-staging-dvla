package uk.gov.moj.cpp.stagingdvla.notifygateway;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.stagingdvla.azure.AzureResourceResolver;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotifyGatewaySenderClientProducerTest {

    @Mock
    private AzureResourceResolver azureResourceResolver;

    @Mock
    private NotifyGatewayServiceBusConfiguration configuration;

    @Mock
    private ServiceBusSenderClient senderClient;

    @InjectMocks
    private NotifyGatewaySenderClientProducer producer;

    @Test
    public void shouldDelegateClientBuildingToTheSharedAzureResourceResolverAndCacheTheResult() {
        when(azureResourceResolver.serviceBusSenderClient(configuration)).thenReturn(senderClient);

        producer.initialise();

        assertThat(producer.notifyGatewaySenderClient(), sameInstance(senderClient));
    }
}
