package uk.gov.moj.cpp.stagingdvla.service;

import static com.azure.core.util.BinaryData.fromStream;
import static com.azure.core.util.Context.NONE;
import static java.util.Map.of;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.CONVERSION_FORMAT;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.FILE_NAME;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.FILE_SIZE;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.NUMBER_OF_PAGES;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.TEMPLATE_NAME;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.blobstore.AzureFileStoreBlobConfiguration;
import uk.gov.moj.cpp.stagingdvla.blobstore.StoragePath;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentGeneratorService {

    private static final StoragePath BLOB_PATH = StoragePath.internal();
    public static final String DVLA_DOCUMENT_TEMPLATE_NAME = "EDT_DriverOutNotification";
    public static final String DVLA_DOCUMENT_ORDER = "DVLADocumentOrder";
    public static final String STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY = "stagingdvla.command.handler.driver-notification-document-delivery";
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorService.class);
    private static final String ERROR_MESSAGE = "Error while uploading document generation or upload ";


    private final SystemDocGeneratorService systemDocGeneratorService;

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private final FileService fileService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private BlobContainerClient blobContainerClient;

    @Inject
    private AzureFileStoreBlobConfiguration azureBlobConfiguration;

    @Inject
    private FeatureControlGuard featureControlGuard;


    @Inject
    public DocumentGeneratorService(
            final FileService fileService,
            final SystemDocGeneratorService systemDocGeneratorService,
            final ObjectToJsonObjectConverter objectToJsonObjectConverter
    ) {
        this.fileService = fileService;
        this.systemDocGeneratorService = systemDocGeneratorService;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void generateDocument(final JsonEnvelope originatingEnvelope, final UUID materialId, final JsonObject payload, final String fileName, final String templateName, final ConversionFormat format, final String originatingSource) {
        try {
            final UUID fileId;
            final Result result;
            if (featureControlGuard.isFeatureEnabled("dvlaFileStore")) {
                result = new Result(null, null);
                fileId = fileService.storePayload(payload, fileName, templateName, format);
            } else {
                fileId = null;
                result = uploadDocumentToAzureBlob(materialId, payload, fileName, format, templateName);
            }
            final DocumentGenerationRequest documentGenerationRequest = new DocumentGenerationRequest(
                    originatingSource,
                    templateName,
                    format,
                    materialId.toString(),
                    fileId,
                    result.payloadFileUri(),
                    result.destinationFileUri());
            systemDocGeneratorService.generateDocument(documentGenerationRequest, originatingEnvelope);

            recordDocumentDeliveryStatus(originatingEnvelope, materialId);
        } catch (RuntimeException e) {
            e.printStackTrace();
            LOGGER.error(ERROR_MESSAGE, e);
        }
    }

    public record Result(String payloadFileUri, String destinationFileUri){}

    public Result uploadDocumentToAzureBlob(UUID materialId, JsonObject payload, String fileName, ConversionFormat format, String templateName ) {
        final BlobClient blobClient = blobContainerClient.getBlobClient(BLOB_PATH.blobName(fileName.replaceAll("\\.[^.]*$", "")));
        final byte[] byteArray = payload.toString().getBytes(StandardCharsets.UTF_8);
                blobClient.uploadWithResponse(
                        new BlobParallelUploadOptions(fromStream(new ByteArrayInputStream(byteArray)))
                .setMetadata(of(
                "correlation_id", materialId.toString(),
        FILE_NAME, fileName,
        CONVERSION_FORMAT, format.toString(),
        TEMPLATE_NAME, templateName,
        NUMBER_OF_PAGES, "1",
        FILE_SIZE, String.valueOf(byteArray.length))),
                azureBlobConfiguration.getTransferTimeout(), NONE);

        return new Result(blobClient.getBlobUrl(), blobClient.getBlobUrl() + "." + fileName.replaceAll(".*\\.", ""));
    }

    private void recordDocumentDeliveryStatus(final JsonEnvelope originatingEnvelope, final UUID materialId) {
        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("materialStatus", DvlaDocumentDeliveryMaterialStatus.PENDING.name())
                .build();

        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(originatingEnvelope.metadata())
                        .withName(STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY),
                payload));
    }

    public String getMaterialIdAmendedFileName(final String fileName, final String materialId) {
        return String.format("%s_%s.pdf", fileName, materialId);
    }
}