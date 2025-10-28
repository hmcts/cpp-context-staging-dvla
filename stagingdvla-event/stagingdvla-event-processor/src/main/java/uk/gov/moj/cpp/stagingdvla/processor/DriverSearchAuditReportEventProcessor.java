package uk.gov.moj.cpp.stagingdvla.processor;

import static java.lang.String.join;
import static java.time.LocalDate.parse;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.CONVERSION_FORMAT;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.CSV;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.DVLA_AUDIT_RECORDS;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.FILE_NAME;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.FILE_SIZE;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.NUMBER_OF_PAGES;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.ORIGINATING_SOURCE;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.PAYLOAD_FILE_SERVICE_ID;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.SOURCE_CORRELATION_ID;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.TEMPLATE_IDENTIFIER;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.TEMPLATE_NAME;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.AUDIT_REPORT_ORIGINATOR_VALUE;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeleted;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportRequested;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportStored;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditRepository;
import uk.gov.moj.cpp.stagingdvla.service.MaterialService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:CallToDeprecatedMethod")
@ServiceComponent(EVENT_PROCESSOR)
public class DriverSearchAuditReportEventProcessor {
    public static final String AUDIT_REPORT_PREFIX = "DriverAuditReport";
    private static final Logger LOGGER = LoggerFactory.getLogger(DriverSearchAuditReportEventProcessor.class.getCanonicalName());
    public static final String DATE_FORMAT = "yyyy-MM-dd_hh-mm-ss";
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private DriverAuditRepository driverAuditRepository;
    @Inject
    private FileStorer fileStorer;
    @Inject
    private FileRetriever fileRetriever;
    @Inject
    private Sender sender;
    @Inject
    private MaterialService materialService;
    @Inject
    private SystemUserProvider userProvider;
    @Inject
    private Enveloper enveloper;

    public static byte[] jsonObjectAsByteArray(final JsonObject jsonObject) {
        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Handles("stagingdvla.event.driver-search-audit-report-requested")
    public void processDriverSearchAuditReportRequested(final JsonEnvelope envelope) throws FileServiceException {
        if(!envelope.payloadIsNull()) {
            final DriverSearchAuditReportRequested auditReportRequested = jsonObjectToObjectConverter
                    .convert(envelope.payloadAsJsonObject(), DriverSearchAuditReportRequested.class);

            final DriverAuditReportSearchCriteria driverAuditReportSearchCriteria = auditReportRequested.getReportSearchCriteria();
            final JsonObject docGeneratorPayload = getAuditReportDocumentGeneratorPayload(driverAuditReportSearchCriteria);

            final UUID fileId = storeAuditReportDocumentGeneratorPayload(docGeneratorPayload,
                    constructFileName(), DVLA_AUDIT_RECORDS);
            LOGGER.info("Sending systemdocgenerator.generate-document request for reportId: {}",
                    auditReportRequested.getId());
            this.requestAuditReportDocumentGeneration(envelope, auditReportRequested.getId().toString(), fileId, DVLA_AUDIT_RECORDS, DVLA_AUDIT_RECORDS);
        }
    }

    @Handles("stagingdvla.event.driver-search-audit-report-created")
    public void processDriverSearchAuditReportCreated(final JsonEnvelope event) {
        if(!event.payloadIsNull()) {
            final DriverSearchAuditReportCreated auditReportCreated = jsonObjectToObjectConverter
                    .convert(event.payloadAsJsonObject(), DriverSearchAuditReportCreated.class);

            final UUID reportId = auditReportCreated.getId();
            final String fileServiceId = auditReportCreated.getReportFileId();
            final Optional<UUID> contextSystemUserId = userProvider.getContextSystemUserId();

            LOGGER.info("Sending material.command.upload-file for reportId: {}", reportId);
            materialService.uploadMaterial(fromString(fileServiceId), auditReportCreated.getMaterialId(),
                    contextSystemUserId.orElse(null), AUDIT_REPORT_ORIGINATOR_VALUE, reportId);
        }
    }

    @Handles("stagingdvla.event.driver-search-audit-report-stored")
    public void handleDriverSearchAuditReportStored(final JsonEnvelope envelope) {
        if(!envelope.payloadIsNull()) {
            final DriverSearchAuditReportStored reportStored = jsonObjectToObjectConverter
                    .convert(envelope.payloadAsJsonObject(), DriverSearchAuditReportStored.class);

            final UUID reportId = reportStored.getId();
            final UUID materialId = reportStored.getMaterialId();

            LOGGER.info("Sending public.stagingdvla.event.driver-search-audit-report-generated for reportId: {}, materialId: {}",
                    reportId, materialId);
            sender.send(Enveloper.envelop(envelope.payloadAsJsonObject())
                    .withName("public.stagingdvla.event.driver-search-audit-report-generated")
                    .withMetadataFrom(envelope));
        }
    }


    @Handles("stagingdvla.event.driver-search-audit-report-deleted")
    public void processDriverSearchAuditReportDeleted(final JsonEnvelope envelope) {
        if(!envelope.payloadIsNull()) {
            final DriverSearchAuditReportDeleted auditReportDeleted = jsonObjectToObjectConverter
                    .convert(envelope.payloadAsJsonObject(), DriverSearchAuditReportDeleted.class);

            final UUID reportId = auditReportDeleted.getId();
            final UUID materialId = auditReportDeleted.getMaterialId();

            LOGGER.info("Sending public.stagingdvla.event.driver-search-audit-report-deleted for reportId: {}, materialId: {}",
                    reportId, materialId);
            sender.send(Enveloper.envelop(envelope.payloadAsJsonObject())
                    .withName("public.stagingdvla.event.driver-search-audit-report-deleted")
                    .withMetadataFrom(envelope));

            LOGGER.info("Sending material.command.delete-material for reportId: {} and materialId: {}",
                    reportId, materialId);
            materialService.sendCommandToDeleteMaterial(envelope, materialId);
        }
    }

    @Handles("stagingdvla.event.driver-search-audit-report-deletion-failed")
    public void processDriverSearchAuditReportDeletionFailed(final JsonEnvelope envelope) {
        LOGGER.info("Sending public.stagingdvla.event.driver-search-audit-report-deletion-failed for reportId: {}",
                envelope.payloadAsJsonObject().getString("id"));

        sender.send(Enveloper.envelop(envelope.payloadAsJsonObject())
                .withName("public.stagingdvla.event.driver-search-audit-report-deletion-failed")
                .withMetadataFrom(envelope));

    }

    private JsonObject getAuditReportDocumentGeneratorPayload(final DriverAuditReportSearchCriteria reportSearchCriteria) {
        final LocalDateTime startDate = parse(reportSearchCriteria.getStartDate()).atTime(LocalTime.MIDNIGHT);
        final LocalDateTime endDate = parse(reportSearchCriteria.getEndDate()).atTime(LocalTime.MAX);

        final List<DriverAuditEntity> driverAuditEntities = driverAuditRepository.
                findAllActiveDriverAuditRecords(startDate, endDate, reportSearchCriteria.getDriverNumber(), reportSearchCriteria.getEmail());

        final JsonArrayBuilder driverAuditRecordArray = createArrayBuilder();

        for (final DriverAuditEntity driverAuditEntity : driverAuditEntities) {
            final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                    .add("Date", driverAuditEntity.getDateTime().toLocalDate().toString())
                    .add("Time", driverAuditEntity.getDateTime().toLocalTime().toString())
                    .add("Reason", driverAuditEntity.getReasonType())
                    .add("Reference", driverAuditEntity.getReference());

            payloadBuilder.add("Driver number", driverAuditEntity.getDrivingLicenseNumber() != null ? driverAuditEntity.getDrivingLicenseNumber() : "");
            payloadBuilder.add("First name", driverAuditEntity.getFirstNames() != null ? driverAuditEntity.getFirstNames() : "");
            payloadBuilder.add("Last name", driverAuditEntity.getLastName() != null ? driverAuditEntity.getLastName() : "");
            payloadBuilder.add("Date of Birth", driverAuditEntity.getDateOfBirth() != null ? driverAuditEntity.getDateOfBirth().toString() : "");
            payloadBuilder.add("Gender", driverAuditEntity.getGender() != null ? driverAuditEntity.getGender() : "");
            payloadBuilder.add("Postcode", driverAuditEntity.getPostcode() != null ? driverAuditEntity.getPostcode() : "");
            payloadBuilder.add("Searched By", driverAuditEntity.getUserEmail() != null ? driverAuditEntity.getUserEmail() : "");

            driverAuditRecordArray.add(payloadBuilder.build());
        }

        return createObjectBuilder()
                .add("driverAuditRecords", driverAuditRecordArray.build())
                .build();
    }


    private UUID storeAuditReportDocumentGeneratorPayload(final JsonObject docGeneratorPayload, final String fileName, final String templateName) throws FileServiceException {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(docGeneratorPayload);

        final JsonObject metadata = createObjectBuilder()
                .add(FILE_NAME, fileName)
                .add(CONVERSION_FORMAT, CSV)
                .add(TEMPLATE_NAME, templateName)
                .add(NUMBER_OF_PAGES, 1)
                .add(FILE_SIZE, jsonPayloadInBytes.length)
                .build();
        return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));
    }

    private String constructFileName() {
        return join("_", AUDIT_REPORT_PREFIX, new SimpleDateFormat(DATE_FORMAT).format(new Date())) + ".csv";
    }

    private void requestAuditReportDocumentGeneration(final JsonEnvelope eventEnvelope,
                                                      final String reportId,
                                                      final UUID payloadFileServiceUUID,
                                                      final String originatingSource,
                                                      final String templateIdentifier) {

        final JsonObject docGeneratorPayload = createObjectBuilder()
                .add(ORIGINATING_SOURCE, originatingSource)
                .add(TEMPLATE_IDENTIFIER, templateIdentifier)
                .add(CONVERSION_FORMAT, CSV)
                .add(SOURCE_CORRELATION_ID, reportId)
                .add(PAYLOAD_FILE_SERVICE_ID, payloadFileServiceUUID.toString())
                .build();

        sender.sendAsAdmin(
                Envelope.envelopeFrom(
                        metadataFrom(eventEnvelope.metadata()).withName("systemdocgenerator.generate-document"),
                        docGeneratorPayload
                )
        );
    }
}