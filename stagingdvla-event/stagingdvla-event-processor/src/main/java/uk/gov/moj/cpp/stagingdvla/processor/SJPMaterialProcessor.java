package uk.gov.moj.cpp.stagingdvla.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.FAILED;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.SUCCESS;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery.sjpCase;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;
import uk.gov.moj.cpp.persistence.repository.DvlaDocumentDeliveryRepository;
import uk.gov.moj.cpp.stagingdvla.blobstore.AzureFileStoreBlobConfiguration;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService;

import java.util.List;

import javax.inject.Inject;

import com.azure.storage.blob.BlobUrlParts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class SJPMaterialProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SJPMaterialProcessor.class);

    private static final String DOCUMENT_URI = "documentUri";
    private static final String DOCUMENT_REFERENCE_URI = "documentReferenceUri";

    @Inject
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    @Inject
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Inject
    private AzureFileStoreBlobConfiguration azureFileStoreBlobConfiguration;

    // SJP publishes this for every case document it files; only a blob-addressed one carries
    // documentUri, and only those filed by stagingdvla have a matching dvla_document_delivery record.
    @Handles("public.sjp.case-document-added")
    public void handleCaseDocumentAddedEvent(final JsonEnvelope envelope) {
        recordSjpStatus(envelope, envelope.payloadAsJsonObject().getString(DOCUMENT_URI, null), SUCCESS);
    }

    // SJP publishes this when the case document is already on the case (duplicate filing).
    @Handles("public.sjp.case-document-addition-failed")
    public void handleCaseDocumentFailedEvent(final JsonEnvelope envelope) {
        recordSjpStatus(envelope, envelope.payloadAsJsonObject().getString(DOCUMENT_URI, null), SUCCESS);
    }

    // SJP publishes this when it refuses the upload (case referred for court hearing / not managed
    // by ATCM); the blob uri comes back as documentReferenceUri.
    @Handles("public.sjp.events.case-document-upload-rejected")
    public void handleCaseDocumentRejectedEvent(final JsonEnvelope envelope) {
        recordSjpStatus(envelope, envelope.payloadAsJsonObject().getString(DOCUMENT_REFERENCE_URI, null), FAILED);
    }

    /**
     * @param documentUri blob uri stagingdvla filed the document with; null for a document stagingdvla
     *                    did not file by blob uri, which is ignored
     */
    private void recordSjpStatus(final JsonEnvelope envelope, final String documentUri,
                                 final DvlaDocumentDeliveryMaterialStatus sjpStatus) {
        final String eventName = envelope.metadata().name();

        if (documentUri == null) {
            LOGGER.debug("Ignoring {} - no document uri", eventName);
            return;
        }

        if (!isInOwnContainer(documentUri)) {
            LOGGER.debug("Ignoring {} - document uri {} is not in container {}", eventName, documentUri, azureFileStoreBlobConfiguration.getContainerName());
            return;
        }

        // the document is in our own container, so stagingdvla filed it and its delivery must be tracked
        final List<DvlaDocumentDeliveryEntity> deliveries = dvlaDocumentDeliveryRepository.findByDocumentBlobUri(documentUri);
        if (deliveries.isEmpty()) {
            throw new RuntimeException(
                    String.format("No DVLA document delivery found for %s with document uri %s", eventName, documentUri));
        }

        deliveries.forEach(delivery -> {
            documentDeliveryStatusService.record(envelope.metadata(),
                    sjpCase(delivery.getMaterialId(), delivery.getCaseId(), delivery.getSjpCorrelationId(), sjpStatus));
        });
    }

    // stagingdvla files SJP documents from its own blob container, so a uri in any other container
    // (or one that is not a blob url at all) belongs to a document some other context filed
    private boolean isInOwnContainer(final String documentUri) {
        try {
            return azureFileStoreBlobConfiguration.getContainerName().equals(BlobUrlParts.parse(documentUri).getBlobContainerName());
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
}
