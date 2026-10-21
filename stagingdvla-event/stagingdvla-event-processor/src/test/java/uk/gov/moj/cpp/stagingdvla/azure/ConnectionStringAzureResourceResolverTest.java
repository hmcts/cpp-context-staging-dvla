package uk.gov.moj.cpp.stagingdvla.azure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.stagingdvla.notifygateway.NotifyGatewayServiceBusConfiguration;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;

public class ConnectionStringAzureResourceResolverTest {

    // placeholder emulator connection string (not a real credential) - same shape testcontainers-azure's
    // Service Bus emulator hands back at runtime
    private static final String SERVICE_BUS_EMULATOR_CONNECTION_STRING =
            "Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;"
                    + "SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

    private final ConnectionStringAzureResourceResolver resolver = new ConnectionStringAzureResourceResolver();

    @Test
    public void shouldBuildServiceBusSenderClientForTheConfiguredConnectionStringAndQueue() {
        final NotifyGatewayServiceBusConfiguration configuration = new NotifyGatewayServiceBusConfiguration();
        setField(configuration, "connectionString", SERVICE_BUS_EMULATOR_CONNECTION_STRING);
        setField(configuration, "sendQueueName", "steccm01.ng-send-email");

        final ServiceBusSenderClient senderClient = resolver.serviceBusSenderClient(configuration);

        assertThat(senderClient.getEntityPath(), is("steccm01.ng-send-email"));
    }
}
