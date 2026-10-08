package uk.gov.moj.cpp.stagingdvla.service;

import static com.azure.core.util.BinaryData.fromStream;
import static com.azure.core.util.Context.NONE;
import static java.util.Map.of;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.PENDING;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.AUDIT_REPORT_PREFIX;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.CONVERSION_FORMAT;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.DVLA_AUDIT_RECORDS;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.FILE_NAME;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.FILE_SIZE;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.NUMBER_OF_PAGES;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.TEMPLATE_NAME;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery.material;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.blobstore.AzureFileStoreBlobConfiguration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    public static final String DVLA_DOCUMENT_TEMPLATE_NAME = "EDT_DriverOutNotification";
    public static final String DVLA_DOCUMENT_ORDER = "DVLADocumentOrder";
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ERROR_MESSAGE = "Error while uploading document generation or upload ";


    private final SystemDocGeneratorService systemDocGeneratorService;

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private final FileService fileService;

    @Inject
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Inject
    private BlobContainerClient blobContainerClient;

    @Inject
    private AzureFileStoreBlobConfiguration azureBlobConfiguration;



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
    public void generateDvlaDocument(final JsonEnvelope originatingEnvelope, final UUID userId, final DriverNotified driverNotified) {
        try {

            final JsonObject nowsDocumentOrderJson = objectToJsonObjectConverter.convert(driverNotified);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("generate D20 for DriverNotified event {}", driverNotified.getIdentifier());
            }

            final String fileName = getTimeStampAmendedFileName(DVLA_DOCUMENT_ORDER);

            final UUID fileId = fileService.storePayload(nowsDocumentOrderJson, fileName, DVLA_DOCUMENT_TEMPLATE_NAME);
            final DocumentGenerationRequest documentGenerationRequest = new DocumentGenerationRequest(
                    DVLA_DOCUMENT_ORDER,
                    DVLA_DOCUMENT_TEMPLATE_NAME,
                    ConversionFormat.PDF,
                    userId.toString(),
                    fileId, null, null);
            systemDocGeneratorService.generateDocument(documentGenerationRequest, originatingEnvelope);

        } catch (RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
        }
    }

    private String getTimeStampAmendedFileName(final String fileName) {
        return String.format("%s_%s.pdf", fileName, ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
    }

    /**
     * Blob-backed driver search audit report (CSV) generation.
     *
     * @param fileName the visible report name, the same one the file-service path uses
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void generateDriverAuditReportDocument(final JsonEnvelope originatingEnvelope, final UUID reportId, final JsonObject payload, final String fileName) {
        uploadAndRequestDocumentGeneration(originatingEnvelope, reportId, payload, fileName,
                DVLA_AUDIT_RECORDS, ConversionFormat.CSV, DVLA_AUDIT_RECORDS, AUDIT_REPORT_PREFIX, null);
    }

    /**
     * Blob-backed D20 generation for a driverNotified payload.
     *
     * @param sjpCaseId the SJP case the document is filed with, null otherwise. It goes on the blob
     *                  PENDING record that starts delivery tracking, so MaterialAggregate knows from
     *                  the outset that the blob must outlive material SUCCESS until SJP has read it -
     *                  rather than learning it from the later sjp PENDING record, which a material
     *                  SUCCESS processed first would overtake (deleting the blob SJP still needs)
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void generateDocument(final JsonEnvelope originatingEnvelope, final UUID materialId, final JsonObject payload, final UUID sjpCaseId) {
        uploadAndRequestDocumentGeneration(originatingEnvelope, materialId, payload, getTimeStampAmendedFileName(DVLA_DOCUMENT_ORDER),
                DVLA_DOCUMENT_TEMPLATE_NAME, ConversionFormat.PDF, DVLA_DOCUMENT_ORDER, DVLA_DOCUMENT_ORDER, sjpCaseId);
    }

    // shared by generateDocument and generateDriverAuditReportDocument; kept un-annotated so neither @Transactional
    // method calls the other - a self-invocation bypasses the CDI proxy and would never apply the callee's REQUIRES_NEW.
    // The payload blob is named <blobNamePrefix>_<materialId>
    @SuppressWarnings("squid:S00107")
    private void uploadAndRequestDocumentGeneration(final JsonEnvelope originatingEnvelope, final UUID materialId, final JsonObject payload, final String fileName, final String templateName, final ConversionFormat format, final String originatingSource, final String blobNamePrefix, final UUID sjpCaseId) {
        final Result result;
        result = uploadDocumentToAzureBlob(materialId, payload, fileName, format, templateName, blobNamePrefix);
        final DocumentGenerationRequest documentGenerationRequest = new DocumentGenerationRequest(
                originatingSource,
                templateName,
                format,
                materialId.toString(),
                null,
                result.payloadFileUri(),
                result.destinationFileUri());
        systemDocGeneratorService.generateDocumentForBlobUIR(documentGenerationRequest, originatingEnvelope);

        documentDeliveryStatusService.record(originatingEnvelope.metadata(),
                material(materialId, PENDING, result.payloadFileUri(), result.destinationFileUri(), sjpCaseId));
    }

    private record Result(String payloadFileUri, String destinationFileUri){}

    private Result uploadDocumentToAzureBlob(UUID materialId, JsonObject payload, String fileName, ConversionFormat format, String templateName, String blobNamePrefix ) {
        final BlobClient blobClient = blobContainerClient.getBlobClient(blobNamePrefix + "_" + materialId);
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

        final String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return new Result(blobClient.getBlobUrl(), blobClient.getBlobUrl() + "." + extension);
    }
}