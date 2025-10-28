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
public class DriverSearchAuditApiTest {
    @Mock
    private Sender sender;
    @InjectMocks
    private DriverSearchAuditApi driverSearchAuditApi;
    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleGenerateDriverSearchAuditReportCommand() {
        final JsonObject payload = createObjectBuilder()
                .add("email", "eltonjohn12345@co.uk")
                .add("driverNumber", "DRIVER1002101212121")
                .add("startDate", "2024-01-01T21:34:09.387Z")
                .add("endDate", "2023-01-02T21:34:09.387Z")
                .build();
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(DriverSearchAuditApi.STAGINGDVLA_COMMAND_GENERATE_DRIVER_SEARCH_AUDIT_REPORT)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        driverSearchAuditApi.generateDriverSearchAuditReport(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is(DriverSearchAuditApi.STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT));
        assertThat(capturedEnvelope.payload(), is(payload));

    }

    @Test
    public void shouldHandleDeleteDriverSearchAuditReportCommand() {
        final JsonObject payload = createObjectBuilder()
                .add("reportFileId", "1002101212121")
                .build();
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(DriverSearchAuditApi.STAGINGDVLA_COMMAND_DELETE_DRIVER_SEARCH_AUDIT_REPORT)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        driverSearchAuditApi.deleteDriverSearchAuditReport(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is(DriverSearchAuditApi.STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT));
        assertThat(capturedEnvelope.payload(), is(payload));

    }
}
