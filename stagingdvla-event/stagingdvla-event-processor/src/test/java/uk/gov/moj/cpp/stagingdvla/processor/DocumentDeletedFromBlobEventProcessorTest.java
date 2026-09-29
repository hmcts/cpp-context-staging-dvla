package uk.gov.moj.cpp.stagingdvla.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentDeletedFromBlobEventProcessorTest {

    private static final String CONTAINER_NAME = "stack-stagingdvla";
    private static final String PAYLOAD_BLOB_NAME = "internal/DVLADocumentOrder";
    private static final String DOCUMENT_BLOB_NAME = PAYLOAD_BLOB_NAME + ".pdf";
    private static final String CONTAINER_URI = "https://sadevfilestore.blob.core.windows.net/" + CONTAINER_NAME + "/";
    private static final String PAYLOAD_URI = CONTAINER_URI + PAYLOAD_BLOB_NAME;
    private static final String DOCUMENT_URI = CONTAINER_URI + DOCUMENT_BLOB_NAME;
    private static final String DVLA_FILE_STORE_DELETE = "dvlaFileStoreDelete";

    @InjectMocks
    private DocumentDeletedFromBlobEventProcessor documentDeletedFromBlobEventProcessor;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Mock
    private BlobClient payloadBlobClient;

    @Mock
    private BlobClient documentBlobClient;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldDeletePayloadAndDocumentBlobsWhenDeleteToggleIsEnabled() {
        when(featureControlGuard.isFeatureEnabled(DVLA_FILE_STORE_DELETE)).thenReturn(true);
        when(blobContainerClient.getBlobClient(PAYLOAD_BLOB_NAME)).thenReturn(payloadBlobClient);
        when(blobContainerClient.getBlobClient(DOCUMENT_BLOB_NAME)).thenReturn(documentBlobClient);
        when(payloadBlobClient.deleteIfExists()).thenReturn(true);
        when(documentBlobClient.deleteIfExists()).thenReturn(true);

        documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(event(PAYLOAD_URI, DOCUMENT_URI));

        verify(payloadBlobClient).deleteIfExists();
        verify(documentBlobClient).deleteIfExists();
    }

    @Test
    public void shouldNotFailWhenBlobsAreAlreadyDeleted() {
        when(featureControlGuard.isFeatureEnabled(DVLA_FILE_STORE_DELETE)).thenReturn(true);
        when(blobContainerClient.getBlobClient(PAYLOAD_BLOB_NAME)).thenReturn(payloadBlobClient);
        when(blobContainerClient.getBlobClient(DOCUMENT_BLOB_NAME)).thenReturn(documentBlobClient);
        when(payloadBlobClient.deleteIfExists()).thenReturn(false);
        when(documentBlobClient.deleteIfExists()).thenReturn(false);

        documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(event(PAYLOAD_URI, DOCUMENT_URI));

        verify(payloadBlobClient).deleteIfExists();
        verify(documentBlobClient).deleteIfExists();
    }

    @Test
    public void shouldNotDeleteBlobsWhenDeleteToggleIsDisabled() {
        when(featureControlGuard.isFeatureEnabled(DVLA_FILE_STORE_DELETE)).thenReturn(false);

        documentDeletedFromBlobEventProcessor.processDocumentDeletedFromBlob(event(PAYLOAD_URI, DOCUMENT_URI));

        verifyNoInteractions(blobContainerClient, payloadBlobClient, documentBlobClient);
        verify(jsonObjectToObjectConverter, never()).convert(any(), any());
    }

    private static JsonEnvelope event(final String payloadBlobUri, final String documentBlobUri) {
        return envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.document-deleted-from-blob"),
                createObjectBuilder()
                        .add("materialId", UUID.randomUUID().toString())
                        .add("payloadBlobUri", payloadBlobUri)
                        .add("documentBlobUri", documentBlobUri)
                        .build());
    }
}
