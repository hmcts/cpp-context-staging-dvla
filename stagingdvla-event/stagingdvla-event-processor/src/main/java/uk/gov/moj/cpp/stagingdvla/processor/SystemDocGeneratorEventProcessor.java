package uk.gov.moj.cpp.stagingdvla.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.FAILED;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.PENDING;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.FILE_NAME;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.isForDriverAuditReportDocument;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.EndorsementType.NEW;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.getEndorsementType;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.hasMultipleConvictingCourts;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.hasMultipleConvictionDates;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery.material;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery.sjpCase;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.cpp.stagingdvla.CaseDocument;
import uk.gov.justice.cpp.stagingdvla.DocumentCategory;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.stagingdvla.SjpDocumentTypes;
import uk.gov.moj.cpp.stagingdvla.service.ApplicationParameters;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService;
import uk.gov.moj.cpp.stagingdvla.service.UploadMaterialContext;
import uk.gov.moj.cpp.stagingdvla.service.UploadMaterialService;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobUrlParts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class SystemDocGeneratorEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDocGeneratorEventProcessor.class.getCanonicalName());
    private static final String DOCUMENT_AVAILABLE_EVENT_NAME = "public.systemdocgenerator.events.document-available";
    private static final String DOCUMENT_GENERATION_FAILED_EVENT_NAME = "public.systemdocgenerator.events.generation-failed";
    public static final String DVLA_DOCUMENT_ORDER = "DVLADocumentOrder";
    public static final String DOCUMENT_FILE_SERVICE_ID = "documentFileServiceId";
    private static final String SUBJECT = "subject";
    private static final String EMAIL_SUBJECT = "DVLA Driver Notification - ";
    private static final String NEW_ENDORSEMENT = "New Endorsement - ";
    private static final String UPDATED_ENDORSEMENT = "Updated Endorsement - ";
    private static final String REMOVAL_OF_ENDORSEMENT = "Removal of Endorsement - ";
    public static final String SOURCE_CORRELATION_ID = "sourceCorrelationId";
    public static final String PAYLOAD_FILE_SERVICE_ID = "payloadFileServiceId";
    public static final String ORIGINATING_SOURCE = "originatingSource";

    private static final String CODE_FOR_SJP_CASE = "J";
    private static final String MATERIAL_ID = "materialId";
    private static final String COURT_DOCUMENT = "courtDocument";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String SJP_UPLOAD_CASE_DOCUMENT = "sjp.upload-case-document";
    private static final String DOCUMENT_TYPE_DESCRIPTION = "Electronic Notifications";
    private static final UUID CASE_DOCUMENT_TYPE_ID = fromString("f471eb51-614c-4447-bd8d-28f9c2815c9e");
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String DESTINATION_FILE_URI = "destinationFileUri";
    private static final String PAYLOAD_FILE_URI = "payloadFileUri";
    @Inject
    private Sender sender;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private FileService fileService;

    @Inject
    private UploadMaterialService uploadMaterialService;

    @Inject
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    // Same bean DocumentGeneratorService.uploadDocumentToAzureBlob writes the driverNotified
    // payload through (AzureFileStoreBlobContainerClientProducer) - reused here to read it back.
    @Inject
    private BlobContainerClient blobContainerClient;

    private record Result(DriverNotified driverNotified, String fileName) {}

    @Handles(DOCUMENT_AVAILABLE_EVENT_NAME)
    public void handleDocumentAvailable(final JsonEnvelope documentAvailableEvent) throws FileServiceException {
        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();
        final String originatingSource = documentAvailablePayload.getString(ORIGINATING_SOURCE, "");
        if (isForDriverAuditReportDocument(originatingSource)) {
            final String fileId = documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID, null);
            final String payloadFileUri = documentAvailablePayload.getString(PAYLOAD_FILE_URI, null);
            final String destinationFileUri = documentAvailablePayload.getString(DESTINATION_FILE_URI, null);
            final String reportId = documentAvailablePayload.getString(SOURCE_CORRELATION_ID);
            LOGGER.info("Sending stagingdvla.command.handler.driver-record-search-audit-report-created for reportId: {}",
                    reportId);
            this.sender.send(Envelope.envelopeFrom(metadataFrom(documentAvailableEvent.metadata())
                            .withName("stagingdvla.command.handler.driver-record-search-audit-report-created")
                            .build(),
                    this.objectToJsonObjectConverter.convert(buildHandleDriverAuditReportDocumentCreation(reportId, fileId, payloadFileUri, destinationFileUri))));
        }

        if (DVLA_DOCUMENT_ORDER.equalsIgnoreCase(originatingSource)) {
            handleDvlaDocumentAvailable(documentAvailableEvent);
        }
    }

    @Handles(DOCUMENT_GENERATION_FAILED_EVENT_NAME)
    public void handleDocumentGenerationFailedEvent(final JsonEnvelope envelope) {
        final JsonObject generationFailedPayload = envelope.payloadAsJsonObject();
        final String originatingSource = generationFailedPayload.getString(ORIGINATING_SOURCE, "");
        final String payloadFileUri = generationFailedPayload.getString(PAYLOAD_FILE_URI, null);
        // only blob-backed stagingdvla documents are tracked (see MaterialAggregate.recordDocumentDelivery)
        if (nonNull(payloadFileUri) && (DVLA_DOCUMENT_ORDER.equalsIgnoreCase(originatingSource) || isForDriverAuditReportDocument(originatingSource))) {
            final String materialId = generationFailedPayload.getString(SOURCE_CORRELATION_ID);

            documentDeliveryStatusService.record(envelope.metadata(), material(fromString(materialId), FAILED));
        }
    }

    private void handleDvlaDocumentAvailable(final JsonEnvelope envelope) throws FileServiceException {
        final JsonObject documentAvailablePayload = envelope.payloadAsJsonObject();

        final String documentFileServiceId = documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID, null);
        final UUID payloadFileId = documentAvailablePayload.containsKey(PAYLOAD_FILE_SERVICE_ID) ? fromString(documentAvailablePayload.getString(PAYLOAD_FILE_SERVICE_ID)) : null;
        final String payloadFileUri = documentAvailablePayload.getString(PAYLOAD_FILE_URI, null);
        final String destinationFileUri = documentAvailablePayload.getString(DESTINATION_FILE_URI, null);
        final String userId = documentAvailablePayload.getString(SOURCE_CORRELATION_ID);
        final Result result = getDriverNotifiedFromDocument(payloadFileId, payloadFileUri);
        final DriverNotified driverNotified = result.driverNotified;

        List<EmailChannel> emailNotifications = null;

        if (shouldSendEmailNotification(driverNotified)) {
            emailNotifications = getEmailNotification(driverNotified);
        }
        UUID generateDocumentFileId = nonNull(documentFileServiceId) ? fromString(documentFileServiceId) : null;

        addDocumentToMaterial(sender, envelope, generateDocumentFileId, fromString(userId), driverNotified.getOrderingHearingId().toString(), driverNotified.getMaterialId(),
                emailNotifications);

        //Sending material as court document to sjp for sjp case or progression for cc case
        final boolean isSJPCase = driverNotified.getCases().stream().map(Cases::getInitiationCode).anyMatch(a -> nonNull(a) && a.equalsIgnoreCase(CODE_FOR_SJP_CASE));
        final Metadata metadata = metadataFrom(envelope.metadata()).withUserId(userId).build();

        final String generateDocFileName = result.fileName;
        if (isSJPCase) {
            generateDocumentFileId = nonNull(documentFileServiceId) ? fromString(documentFileServiceId) : UUID.nameUUIDFromBytes(destinationFileUri.getBytes(StandardCharsets.UTF_8));
            addCourtDocumentForSjpCase(metadata, driverNotified, generateDocFileName, generateDocumentFileId, destinationFileUri);
        } else {
            addCourtDocumentForCCCase(sender, metadata, driverNotified, generateDocFileName);
        }
    }

    private Result getDriverNotifiedFromDocument(final UUID payloadFileId, final String payloadFileUri) throws FileServiceException {
        if(nonNull(payloadFileId)){
            final FileReference payloadFileReference = fileService.retrieve(payloadFileId).orElseThrow(() -> new BadRequestException("Failed to retrieve file"));
            try (payloadFileReference; final JsonReader reader = createReader(payloadFileReference.getContentStream())) {
                final JsonObject rawPayload = reader.readObject();
                return new Result(jsonObjectToObjectConverter.convert(rawPayload, DriverNotified.class), payloadFileReference.getMetadata().getString(FILE_NAME));
            }
        } else {
            final String blobName = BlobUrlParts.parse(payloadFileUri).getBlobName();
            final BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

            try (JsonReader reader = createReader(blobClient.downloadContent().toStream())) {
                final JsonObject rawPayload = reader.readObject();

                final String fileName = blobClient.getProperties().getMetadata().get(FILE_NAME.toLowerCase());

                return new Result(jsonObjectToObjectConverter.convert(rawPayload, DriverNotified.class), fileName);
            }
        }
    }

    private static boolean shouldSendEmailNotification(final DriverNotified driverNotified) {
        return !NEW.equals(getEndorsementType(driverNotified))
                || hasMultipleConvictionDates(driverNotified)
                || hasMultipleConvictingCourts(driverNotified);
    }


    private List<EmailChannel> getEmailNotification(final DriverNotified driverNotified) {
        final String templateId = applicationParameters.getDvlaEmailTemplateId();
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(driverNotified.getMaterialId());
        final List<EmailChannel> emailNotifications = new ArrayList<>();
        final EmailChannel emailChannel = EmailChannel.emailChannel()
                .withMaterialUrl(materialUrl)
                .withPersonalisation(buildPersonalisation(getEmailSubject(driverNotified)))
                .withSendToAddress(getEmailAddress(driverNotified))
                .withTemplateId(fromString(templateId)).build();
        emailNotifications.add(emailChannel);
        return emailNotifications;
    }

    private Personalisation buildPersonalisation(final String subject) {
        return Personalisation.personalisation()
                .withAdditionalProperty(SUBJECT, subject).build();
    }

    private String getEmailAddress(final DriverNotified driverNotified) {
        if (isNotEmpty(driverNotified.getRemovedEndorsements()) || isNotEmpty(driverNotified.getUpdatedEndorsements())) {
            return applicationParameters.getDvlaEmailAddress2();
        } else {
            return applicationParameters.getDvlaEmailAddress1();
        }
    }

    private String getEmailSubject(final DriverNotified driverNotified) {
        final StringBuilder sb = new StringBuilder(EMAIL_SUBJECT);
        if (isNotEmpty(driverNotified.getRemovedEndorsements())) {
            sb.append(REMOVAL_OF_ENDORSEMENT);
        } else if (isNotEmpty(driverNotified.getUpdatedEndorsements())) {
            sb.append(UPDATED_ENDORSEMENT);
        } else {
            sb.append(NEW_ENDORSEMENT);
        }
        sb.append(driverNotified.getDefendant().getLastName());
        sb.append(", ");
        sb.append(driverNotified.getDefendant().getFirstName());
        return sb.toString();
    }

    /**
     * @param originatingEnvelope the document-available event. Its payloadFileUri / destinationFileUri
     *                            are null on the file-service path; when destinationFileUri is set, it
     *                            is the reference that reaches material instead of {@code fileId}.
     */
    private void addDocumentToMaterial(Sender sender, JsonEnvelope originatingEnvelope, final UUID fileId,
                                       final UUID userId, final String hearingId,
                                       final UUID materialId,
                                       final List<EmailChannel> emailNotifications) {
        final JsonObject documentAvailablePayload = originatingEnvelope.payloadAsJsonObject();

        uploadMaterialService.uploadFile(new UploadMaterialContext()
                .setSender(sender)
                .setOriginatingEnvelope(originatingEnvelope)
                .setUserId(userId)
                .setHearingId(fromString(hearingId))
                .setMaterialId(materialId)
                .setFileId(fileId)
                .setCaseId(null)
                .setApplicationId(null)
                .setEmailNotifications(emailNotifications)
                .setPayloadFileUri(documentAvailablePayload.getString(PAYLOAD_FILE_URI, null))
                .setDestinationFileUri(documentAvailablePayload.getString(DESTINATION_FILE_URI, null))
                .build());
    }

    private DriverRecordSearchAuditReportCreated buildHandleDriverAuditReportDocumentCreation(final String reportId, final String fileId, final String payloadFileUri, final String destinationFileUri) {
        return new DriverRecordSearchAuditReportCreated.Builder()
                .withId(fromString(reportId))
                .withReportFileId(fileId)
                .withPayloadFileUri(payloadFileUri)
                .withDestinationFileUri(destinationFileUri)
                .build();
    }

    private void addCourtDocumentForSjpCase(final Metadata metadata, final DriverNotified driverNotified, final String fileName, final UUID fileId, final String caseDocumentUri) {
        driverNotified.getCases().forEach(c -> {
            final JsonObjectBuilder uploadCaseDocumentPayload = createObjectBuilder()
                    .add("caseId", c.getCaseId().toString())
                    .add("caseDocumentType", SjpDocumentTypes.ELECTRONIC_NOTIFICATIONS.name() + "-" + fileName)
                    .add("caseDocument", fileId.toString());
            if (nonNull(caseDocumentUri)) {
                uploadCaseDocumentPayload.add("caseDocumentUri", caseDocumentUri);
            }

            final Envelope<JsonObject> envelope = Envelope.envelopeFrom(
                    JsonEnvelope.metadataFrom(metadata).withName(SJP_UPLOAD_CASE_DOCUMENT),
                    uploadCaseDocumentPayload.build());

            sender.send(envelope);
            documentDeliveryStatusService.record(metadata, sjpCase(driverNotified.getMaterialId(), c.getCaseId(), fileId, PENDING));
        });
    }

    private void addCourtDocumentForCCCase(final Sender sender, final Metadata metadata, final DriverNotified driverNotified, final String fileName) {
        driverNotified.getCases().stream()
                .map(prosecutionCase -> {
                    final CourtDocument courtDocument = buildCourtDocument(prosecutionCase.getCaseId(), driverNotified.getMaterialId(), fileName);

                    final JsonObject jsonObject = createObjectBuilder()
                            .add(MATERIAL_ID, driverNotified.getMaterialId().toString())
                            .add(COURT_DOCUMENT, objectToJsonObjectConverter.convert(courtDocument))
                            .build();

                    return Envelope.envelopeFrom(
                            JsonEnvelope.metadataFrom(metadata)
                                    .withName(PROGRESSION_ADD_COURT_DOCUMENT),
                            jsonObject);
                })
                .forEach(sender::send);
    }

    private CourtDocument buildCourtDocument(final UUID caseId, final UUID materialId, final String fileName) {

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withCaseDocument(CaseDocument.caseDocument()
                        .withProsecutionCaseId(caseId)
                        .build())
                .build();

        final Material material = Material.material().withId(materialId)
                .withReceivedDateTime(ZonedDateTime.now())
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(CASE_DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(fileName)
                .withMaterials(Collections.singletonList(material))
                .withSendToCps(false)
                .withContainsFinancialMeans(false)
                .build();
    }

}
