package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;

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
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

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

        verify(sender).send(envelopeArgumentCaptor.capture());
    }

}
