package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.util.UUID;

import javax.json.JsonObject;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemDocGeneratorServiceTest {

    private static final String ORIGINATING_SOURCE = "DVLADocumentOrder";
    private static final String TEMPLATE_IDENTIFIER = "EDT_DriverOutNotification";

    @Mock
    private Sender sender;

    private final SystemDocGeneratorService systemDocGeneratorService = new SystemDocGeneratorService();

    @BeforeEach
    public void setUp() {
        setField(systemDocGeneratorService, "sender", sender);
    }

    @Test
    public void shouldSendPayloadFileServiceIdWhenPayloadFileServiceIdIsPresent() {
        final UUID payloadFileServiceId = randomUUID();
        final String sourceCorrelationId = randomUUID().toString();
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.PDF, sourceCorrelationId, payloadFileServiceId, null, null);

        final JsonEnvelope originatingEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"), createObjectBuilder().build());

        systemDocGeneratorService.generateDocument(request, originatingEnvelope);

        final ArgumentCaptor<Envelope> envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(envelopeCaptor.capture());

        final Envelope<JsonObject> sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata().name(), is("systemdocgenerator.generate-document"));

        final JsonObject payload = sentEnvelope.payload();
        assertThat(payload.getString("originatingSource"), is(ORIGINATING_SOURCE));
        assertThat(payload.getString("templateIdentifier"), is(TEMPLATE_IDENTIFIER));
        assertThat(payload.getString("conversionFormat"), is(ConversionFormat.PDF.getValue()));
        assertThat(payload.getString("sourceCorrelationId"), is(sourceCorrelationId));
        assertThat(payload.getString("payloadFileServiceId"), is(payloadFileServiceId.toString()));
        assertThat(payload.containsKey("payloadFileUri"), is(false));
        assertThat(payload.containsKey("destinationFileUri"), is(false));
    }

    @Test
    public void shouldSendPayloadAndDestinationFileUriWhenPayloadFileServiceIdIsAbsent() {
        final String sourceCorrelationId = randomUUID().toString();
        final String payloadFileUri = "https://mystorage.blob.core.windows.net/internal/" + randomUUID();
        final String destinationFileUri = payloadFileUri + "-out";
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.PDF, sourceCorrelationId, null, payloadFileUri, destinationFileUri);

        final JsonEnvelope originatingEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"), createObjectBuilder().build());

        systemDocGeneratorService.generateDocument(request, originatingEnvelope);

        final ArgumentCaptor<Envelope> envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(envelopeCaptor.capture());

        final Envelope<JsonObject> sentEnvelope = envelopeCaptor.getValue();
        final JsonObject payload = sentEnvelope.payload();
        assertThat(payload.getString("originatingSource"), is(ORIGINATING_SOURCE));
        assertThat(payload.getString("templateIdentifier"), is(TEMPLATE_IDENTIFIER));
        assertThat(payload.getString("conversionFormat"), is(ConversionFormat.PDF.getValue()));
        assertThat(payload.getString("sourceCorrelationId"), is(sourceCorrelationId));
        assertThat(payload.getString("payloadFileUri"), is(payloadFileUri));
        assertThat(payload.getString("destinationFileUri"), is(destinationFileUri));
        assertThat(payload.containsKey("payloadFileServiceId"), is(false));
    }

    @Test
    public void shouldPreserveOriginatingEnvelopeMetadataOnGeneratedCommand() {
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.PDF, randomUUID().toString(), randomUUID(), null, null);
        final JsonEnvelope originatingEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"), createObjectBuilder().build());

        systemDocGeneratorService.generateDocument(request, originatingEnvelope);

        final ArgumentCaptor<Envelope> envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().metadata().streamId(), is(originatingEnvelope.metadata().streamId()));
    }
}
