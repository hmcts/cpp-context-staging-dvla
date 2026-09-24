package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.stagingdvla.exception.UserNotFoundException;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialServiceTest {

    private static final String DOCUMENT_URI = "https://sadevfilestore.blob.core.windows.net/stack-stagingdvla/internal/DVLADocumentOrder.pdf";

    @InjectMocks
    private MaterialService service;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    Metadata metadata;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldCallMaterialUpload() {
        final String userId = UUID.randomUUID().toString();
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(userId));

        service.uploadMaterial(randomUUID(), randomUUID(), envelope);

        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldCallUploadMaterial() {
        final UUID userId = UUID.randomUUID();

        service.uploadMaterial(randomUUID(), randomUUID(), userId, "source", randomUUID());

        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldCallSendCommandToDeleteMaterial() {
        final UUID materialId = UUID.randomUUID();

        when(envelope.metadata()).thenReturn(metadataWithRandomUUID("some.event").build());
        service.sendCommandToDeleteMaterial(envelope, materialId);

        verify(sender).send(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldUploadMaterial() {
        assertThrows(UserNotFoundException.class, () -> service.uploadMaterial(randomUUID(), randomUUID(), (UUID) null));
    }

    @Test
    public void shouldSendFileUriAndNotFileServiceIdForABlobAddressedDocument() {
        final UUID materialId = randomUUID();

        service.uploadMaterialFromUri(DOCUMENT_URI, materialId, randomUUID());

        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());

        final JsonObject payload = jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject();
        assertThat(payload.getString("fileUri"), is(DOCUMENT_URI));
        assertThat(payload.getString("materialId"), is(materialId.toString()));

        // material.command.upload-file is an exclusive oneOf and its handler rejects a command
        // carrying more than one reference, so fileServiceId must be absent - not null.
        assertThat(payload.containsKey("fileServiceId"), is(false));
    }

    @Test
    public void shouldResolveUserIdFromTheEnvelopeWhenUploadingFromAUri() {
        final String userId = UUID.randomUUID().toString();
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(userId));

        service.uploadMaterialFromUri(DOCUMENT_URI, randomUUID(), envelope);

        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("fileUri"), is(DOCUMENT_URI));
    }

    @Test
    public void shouldRejectAUriUploadWithNoUserId() {
        assertThrows(UserNotFoundException.class, () -> service.uploadMaterialFromUri(DOCUMENT_URI, randomUUID(), (UUID) null));
    }

}
