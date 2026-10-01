package uk.gov.moj.cpp.stagingdvla.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.cpp.stagingdvla.event.DocumentDeletedFromBlob;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobUrlParts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class DocumentDeletedFromBlobEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDeletedFromBlobEventProcessor.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    // Same bean DocumentGeneratorService.uploadDocumentToAzureBlob writes both blobs through
    // (AzureFileStoreBlobContainerClientProducer) - reused here to delete them.
    @Inject
    private BlobContainerClient blobContainerClient;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Handles("stagingdvla.event.document-deleted-from-blob")
    public void processDocumentDeletedFromBlob(final JsonEnvelope envelope) {
        if (!featureControlGuard.isFeatureEnabled("dvlaFileStoreDelete")) {
            LOGGER.info("dvlaFileStoreDelete is false. The file won't be delete.");
            return;
        }
        final DocumentDeletedFromBlob documentDeletedFromBlob = jsonObjectToObjectConverter
                .convert(envelope.payloadAsJsonObject(), DocumentDeletedFromBlob.class);

        deleteBlob(documentDeletedFromBlob.getPayloadBlobUri());
        deleteBlob(documentDeletedFromBlob.getDocumentBlobUri());
    }

    // deleteIfExists keeps a redelivered event harmless; a blob outside our own container was not
    // written by stagingdvla, so it is never resolved against (and deleted from) this container
    private void deleteBlob(final String blobUri) {
        final BlobUrlParts blobUrlParts = BlobUrlParts.parse(blobUri);
        blobContainerClient.getBlobClient(blobUrlParts.getBlobName()).deleteIfExists();
    }
}
