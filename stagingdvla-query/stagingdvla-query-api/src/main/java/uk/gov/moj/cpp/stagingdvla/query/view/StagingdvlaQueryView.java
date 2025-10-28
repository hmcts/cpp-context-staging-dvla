package uk.gov.moj.cpp.stagingdvla.query.view;

import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;
import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;
import uk.gov.moj.cpp.persistence.entity.DrivingConvictionRetryEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditReportRepository;
import uk.gov.moj.cpp.persistence.repository.DriverAuditRepository;
import uk.gov.moj.cpp.persistence.repository.DrivingConvictionRetryRepository;
import uk.gov.moj.cpp.stagingdvla.domain.DrivingConvictionRetry;
import uk.gov.moj.cpp.stagingdvla.query.view.converter.DrivingConvictionRetryConverter;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverAuditQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.service.MaterialService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

public class StagingdvlaQueryView {

    private static final Logger LOGGER = getLogger(StagingdvlaQueryView.class);
    private static final DateTimeFormatter DATE_TIME_FOMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
    private static final DateTimeFormatter FOMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter DATE_FOMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String FIELD_MAX_COUNT = "maxCount";
    private static final String FIELD_DRIVING_CONVICTION_RETRIES = "drivingConvictionRetries";

    @Inject
    private DrivingConvictionRetryRepository drivingConvictionRetryRepository;

    @Inject
    private DrivingConvictionRetryConverter drivingConvictionRetryConverter;

    @Inject
    private DriverAuditRepository driverAuditRepository;

    @Inject
    private DriverAuditReportRepository driverAuditReportRepository;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Inject
    private ObjectToJsonValueConverter jsonValueConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private MaterialService materialService;


    public Envelope<JsonObject> findDrivingConvictionRetries(final JsonEnvelope envelope) {
        final int maxCount = envelope.payloadAsJsonObject().isNull(FIELD_MAX_COUNT)
                ? 0 : envelope.payloadAsJsonObject().getInt(FIELD_MAX_COUNT);

        final List<DrivingConvictionRetryEntity> entities = maxCount > 0
                ? drivingConvictionRetryRepository.findAll(0, maxCount)
                : drivingConvictionRetryRepository.findAll();
        final List<DrivingConvictionRetry> response = drivingConvictionRetryConverter.convert(entities);

        return envelop(createObjectBuilder()
                .add(FIELD_DRIVING_CONVICTION_RETRIES, listToJsonArrayConverter.convert(response)).build())
                .withName("stagingdvla.query.driving-conviction-retry")
                .withMetadataFrom(envelope);
    }

    public Envelope<JsonObject> getDriverAuditRecords(final Envelope<DriverAuditQueryParameters> envelope) {
        final DriverAuditQueryParameters auditQueryParameters = envelope.payload();

        final LocalDateTime startDate = parse(auditQueryParameters.getStartDate()).atTime(LocalTime.MIDNIGHT);
        final LocalDateTime endDate = parse(auditQueryParameters.getEndDate()).atTime(LocalTime.MAX);
        final String drivingLicenseNumber = isNotBlank(auditQueryParameters.getDriverNumber()) ? auditQueryParameters.getDriverNumber() : null;
        final String userEmail = isNotBlank(auditQueryParameters.getEmail()) ? auditQueryParameters.getEmail().toLowerCase() : null;

        final List<DriverAuditEntity> driverAuditRecords = driverAuditRepository.
                findAllActiveDriverAuditRecords(startDate, endDate, drivingLicenseNumber, userEmail);

        return envelop(buildAuditRecordsPayload(driverAuditRecords))
                .withName("stagingdvla.query.driver-audit-records")
                .withMetadataFrom(envelope);
    }

    public Envelope getDriverAuditSearchReports(final JsonEnvelope envelope) {
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("No UserId Supplied")));
        final List<DriverAuditReportEntity> driverAuditReportEntities = driverAuditReportRepository.findByUserIdOrderByDateTimeDesc(userId);
        return envelop(buildAuditReportsPayload(driverAuditReportEntities))
                .withName("stagingdvla.query.driver-search-audit-reports")
                .withMetadataFrom(envelope);
    }

    private JsonObject buildAuditRecordsPayload(final List<DriverAuditEntity> driverAuditEntities) {
        final JsonArrayBuilder driverAuditRecordArray = createArrayBuilder();

        for (final DriverAuditEntity driverAuditEntity : driverAuditEntities) {
            final JsonObjectBuilder payloadBuilder = createObjectBuilder();

            addToPayloadIfNotNull(payloadBuilder, "date", driverAuditEntity.getDateTime().toLocalDate().toString());
            addToPayloadIfNotNull(payloadBuilder, "time", driverAuditEntity.getDateTime().toLocalTime().toString());
            addToPayloadIfNotNull(payloadBuilder, "reason", driverAuditEntity.getReasonType());
            addToPayloadIfNotNull(payloadBuilder, "reference", driverAuditEntity.getReference());
            addToPayloadIfNotNull(payloadBuilder, "driverNumber", driverAuditEntity.getDrivingLicenseNumber());
            addToPayloadIfNotNull(payloadBuilder, "firstName", driverAuditEntity.getFirstNames());
            addToPayloadIfNotNull(payloadBuilder, "lastName", driverAuditEntity.getLastName());
            addToPayloadIfNotNull(payloadBuilder, "dateofBirth", driverAuditEntity.getDateOfBirth());
            addToPayloadIfNotNull(payloadBuilder, "gender", driverAuditEntity.getGender());
            addToPayloadIfNotNull(payloadBuilder, "postcode", driverAuditEntity.getPostcode());
            addToPayloadIfNotNull(payloadBuilder, "searchedBy", driverAuditEntity.getUserEmail());

            driverAuditRecordArray.add(payloadBuilder.build());
        }

        return createObjectBuilder()
                .add("driverAuditRecords", driverAuditRecordArray.build())
                .build();
    }

    private JsonObject buildAuditReportsPayload(final List<DriverAuditReportEntity> driverAuditReportEntities) {
        final JsonArrayBuilder driverAuditReportArray = createArrayBuilder();

        for (final DriverAuditReportEntity driverAuditReportEntity : driverAuditReportEntities) {
            final DriverAuditReportSearchCriteria reportSearchCriteria = jsonObjectToObjectConverter
                    .convert(stringToJsonObjectConverter.convert(driverAuditReportEntity.getReportSearchCriteria()),
                            DriverAuditReportSearchCriteria.class);

            final JsonObjectBuilder payloadBuilder = createObjectBuilder();
            addToPayloadIfNotNull(payloadBuilder, "id", driverAuditReportEntity.getId().toString());
            addToPayloadIfNotNull(payloadBuilder, "searchDateTime", driverAuditReportEntity.getDateTime().format(DATE_TIME_FOMATTER));
            addToPayloadIfNotNull(payloadBuilder, "status", driverAuditReportEntity.getStatus());
            addToPayloadIfNotNull(payloadBuilder, "reportFileId", driverAuditReportEntity.getReportFileId());
            ofNullable(driverAuditReportEntity.getMaterialId()).ifPresent(materialId -> payloadBuilder.add("materialId", materialId.toString()));


            if (nonNull(reportSearchCriteria)) {
                addToPayloadIfNotNull(payloadBuilder, "searchedByEmail", reportSearchCriteria.getEmail());
                addToPayloadIfNotNull(payloadBuilder, "searchedDriverNumber", reportSearchCriteria.getDriverNumber());
                addToPayloadIfNotNull(payloadBuilder, "dateFrom", LocalDate.parse(reportSearchCriteria.getStartDate(), DATE_FOMATTER).format(FOMATTER));
                addToPayloadIfNotNull(payloadBuilder, "dateUntil", LocalDate.parse(reportSearchCriteria.getEndDate(), DATE_FOMATTER).format(FOMATTER));
            }
            driverAuditReportArray.add(payloadBuilder.build());
        }

        return createObjectBuilder()
                .add("driverSearchAuditReports", driverAuditReportArray.build())
                .build();
    }

    public Response getDriverAuditReportByMaterialId(final String id, final String materialId, final String userId) {
        final DriverAuditReportEntity driverAuditReportEntity = driverAuditReportRepository.findByIdAndUserIdAndMaterialId(fromString(id), fromString(userId), fromString(materialId));

        if (Objects.isNull(driverAuditReportEntity)) {
            LOGGER.info("User {} is not authorized to download the report {}", userId, id);
            throw new ForbiddenRequestException("User is not authorize to download this report");
        }
        return materialService.getMaterialResource(materialId);

    }

    private void addToPayloadIfNotNull(JsonObjectBuilder builder, String key, Object value) {
        if (value != null) {
            builder.add(key, value.toString());
        } else {
            builder.addNull(key);
        }
    }
}
