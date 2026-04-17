package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
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

}
