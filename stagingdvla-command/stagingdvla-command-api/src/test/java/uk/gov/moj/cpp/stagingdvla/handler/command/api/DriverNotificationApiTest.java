package uk.gov.moj.cpp.stagingdvla.handler.command.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DriverNotificationApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private DriverNotificationApi api;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleDriverNotificationCommand() {
        final JsonObject payload = createObjectBuilder()
                .add("masterDefendantId", createObjectBuilder().build())
                .add("orderingCourt", createObjectBuilder().build())
                .add("nowContent", createObjectBuilder().build())
                .add("orderingHearingId", createObjectBuilder().build())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(DriverNotificationApi.STAGINGDVLA_COMMAND_DRIVER_NOTIFICATION)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        api.notifyDriver(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is(DriverNotificationApi.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION));
        assertThat(capturedEnvelope.payload(), is(payload));

    }
}
