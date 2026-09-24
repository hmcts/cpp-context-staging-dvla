package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * materialDetails.json declares an exclusive oneOf - either fileId, or payloadFileUri plus
 * destinationFileUri - with additionalProperties false. These tests pin that the context this
 * service builds lands on exactly one branch, because a mixed payload is rejected downstream at
 * schema validation rather than here.
 */
@ExtendWith(MockitoExtension.class)
public class UploadMaterialServiceTest {

    private static final String PAYLOAD_URI = "https://sadevfilestore.blob.core.windows.net/stack-stagingdvla/internal/DVLADocumentOrder";
    private static final String DESTINATION_URI = PAYLOAD_URI + ".pdf";

    @InjectMocks
    private UploadMaterialService uploadMaterialService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @BeforeEach
    public void setUp() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCarryTheFileIdAndNeitherUriForAFileServiceAddressedDocument() {
        uploadMaterialService.uploadFile(contextBuilder().setFileId(randomUUID()).build());

        final JsonObject context = capturedContext();

        assertThat(context.containsKey("fileId"), is(true));
        assertThat(context.containsKey("payloadFileUri"), is(false));
        assertThat(context.containsKey("destinationFileUri"), is(false));
    }

    @Test
    public void shouldCarryBothUrisAndNoFileIdForABlobAddressedDocument() {
        uploadMaterialService.uploadFile(contextBuilder()
                .setFileId(randomUUID())
                .setPayloadFileUri(PAYLOAD_URI)
                .setDestinationFileUri(DESTINATION_URI)
                .build());

        final JsonObject context = capturedContext();

        assertThat(context.getString("payloadFileUri"), is(PAYLOAD_URI));
        assertThat(context.getString("destinationFileUri"), is(DESTINATION_URI));

        // a fileId was supplied on the context, but the uri branch must not carry it - the oneOf
        // is exclusive and material rejects a command with more than one reference.
        assertThat(context.containsKey("fileId"), is(false));
    }

    private UploadMaterialContext contextBuilder() {
        final UUID id = randomUUID();
        return new UploadMaterialContext()
                .setSender(sender)
                .setOriginatingEnvelope(originatingEnvelope())
                .setUserId(id)
                .setHearingId(id)
                .setMaterialId(id)
                .setCaseId(null)
                .setApplicationId(null)
                .setEmailNotifications(null);
    }

    private JsonEnvelope originatingEnvelope() {
        return envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                createObjectBuilder().build());
    }

    private JsonObject capturedContext() {
        verify(sender).send(envelopeCaptor.capture());
        return envelopeCaptor.getValue().payload().getJsonObject("context");
    }
}
