package uk.gov.moj.cpp.stagingdvla.azure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.stagingdvla.notifygateway.NotifyGatewayServiceBusConfiguration;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;

public class ManagedIdentityAzureResourceResolverTest {

    private final ManagedIdentityAzureResourceResolver resolver = new ManagedIdentityAzureResourceResolver();

    @Test
    public void shouldBuildServiceBusSenderClientForTheConfiguredNamespaceAndQueue() {
        final NotifyGatewayServiceBusConfiguration configuration = new NotifyGatewayServiceBusConfiguration();
        setField(configuration, "fullyQualifiedNamespace", "sbsteccm01.servicebus.windows.net");
        setField(configuration, "sendQueueName", "steccm01.ng-send-email");

        final ServiceBusSenderClient senderClient = resolver.serviceBusSenderClient(configuration);

        assertThat(senderClient.getFullyQualifiedNamespace(), is("sbsteccm01.servicebus.windows.net"));
        assertThat(senderClient.getEntityPath(), is("steccm01.ng-send-email"));
    }
}
