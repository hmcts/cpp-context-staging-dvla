package uk.gov.moj.cpp.stagingdvla.query.api;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DriverAuditTypes;
import uk.gov.moj.cpp.stagingdvla.query.api.service.UserDetails;
import uk.gov.moj.cpp.stagingdvla.query.api.service.UserGroupService;
import uk.gov.moj.cpp.stagingdvla.query.view.StagingdvlaQueryView;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverAuditContent;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverAuditQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverImageQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverSummaryQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.SearchCriteria;
import uk.gov.moj.cpp.stagingdvla.query.view.request.SearchReason;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverImageResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverSummaryResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.service.DriverService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.BadRequestException;

import java.util.regex.Pattern;

@ServiceComponent(Component.QUERY_API)
public class StagingdvlaQueryApi {

    private static final String STAGINGDVLA_COMMAND_HANDLER_AUDIT_DRIVER_RECORD = "stagingdvla.command.handler.audit-driver-record";
    private static final String DRIVER_RESPONSE = "stagingdvla.query.driver-response";
    private static final String DRIVER_SUMMARY_RESPONSE = "stagingdvla.query.driver-summary-response";
    private static final String DRIVER_IMAGE_RESPONSE = "stagingdvla.query.driver-image-response";
    private static final Pattern UK_POSTCODE_PATTERN = Pattern.compile(
            "^(GIR 0AA|(?:(?:[A-Z]{1,2}[0-9][0-9A-Z]?)|(?:[A-Z][A-Z][0-9][0-9A-Z]?)) ?[0-9][A-Z]{2})$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DRIVER_NUMBER_PATTERN = Pattern.compile(
            "^[A-Z9]{5}\\d{6}[A-Z9]{2}\\d[A-Z0-9]{2}$"
    );

    @Inject
    private DriverService driverService;

    @Inject
    private StagingdvlaQueryView stagingdvlaQueryView;

    @Inject
    Sender sender;

    @Inject
    UserGroupService userGroupService;


    @Handles("stagingdvla.query.drivernumber")
    public Envelope<DriverResponse> findByDriverNumber(final Envelope<DriverQueryParameters> envelope) {

        final DriverQueryParameters queryParameters = envelope.payload();
        if(! DRIVER_NUMBER_PATTERN.matcher(queryParameters.getDriverNumber()).matches()){
            throw new BadRequestException("driver Number is Invalid");
        }
        final DriverResponse driverResponse = driverService.findByDriverNumber(queryParameters);

        processDriverAuditInformation(queryParameters, null, envelope.metadata());

        return envelop(driverResponse)
                .withName(DRIVER_RESPONSE)
                .withMetadataFrom(envelope);
    }

    @Handles("stagingdvla.query.driverdetails")
    public Envelope<DriverSummaryResponse> findByDriverDetails(final Envelope<DriverSummaryQueryParameters> envelope) {

        final DriverSummaryQueryParameters summaryQueryParameters = envelope.payload();

        if(nonNull(summaryQueryParameters.getDateOfBirth())){
           try {
               LocalDate.parse(summaryQueryParameters.getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE);
           } catch (Exception e) {
               throw new BadRequestException(e);
           }
        }

        if(nonNull(summaryQueryParameters.getPostcode()) && !UK_POSTCODE_PATTERN.matcher(summaryQueryParameters.getPostcode()).matches()){
            throw new BadRequestException("Post code is Invalid");
        }

        final DriverSummaryResponse driverSummaryResponse = driverService.findByDriverDetails(summaryQueryParameters);

        processDriverAuditInformation(null, summaryQueryParameters, envelope.metadata());

        return envelop(driverSummaryResponse)
                .withName(DRIVER_SUMMARY_RESPONSE)
                .withMetadataFrom(envelope);
    }

    @Handles("stagingdvla.query.driving-conviction-retry")
    public Envelope<JsonObject> findDrivingConvictionRetries(final JsonEnvelope query) {
        return stagingdvlaQueryView.findDrivingConvictionRetries(query);
    }

    @Handles("stagingdvla.query.driverimage")
    public Envelope<DriverImageResponse> findDriverImages(final Envelope<DriverImageQueryParameters> envelope) {

        final DriverImageQueryParameters queryParameters = envelope.payload();

        return envelop(driverService.findDriverImage(queryParameters))
                .withName(DRIVER_IMAGE_RESPONSE)
                .withMetadataFrom(envelope);
    }

    @Handles("stagingdvla.query.driver-audit-records")
    public Envelope findDriverAuditRecords(final Envelope<DriverAuditQueryParameters> envelope) {
        return stagingdvlaQueryView.getDriverAuditRecords(envelope);
    }

    @Handles("stagingdvla.query.driver-search-audit-reports")
    public Envelope findDriverAuditSearchReports(final JsonEnvelope envelope) {
        return stagingdvlaQueryView.getDriverAuditSearchReports(envelope);
    }

    public void processDriverAuditInformation(final DriverQueryParameters queryParameters,
                                              final DriverSummaryQueryParameters summaryQueryParameters,
                                              final Metadata metadata) {

        final UserDetails userDetails = userGroupService.getUserById(metadata);

        SearchReason searchReason = null;
        SearchCriteria searchCriteria = null;
        if (nonNull(queryParameters)) {
            searchReason = buildSearchReason(queryParameters.getReasonType(), queryParameters.getReference());
            searchCriteria = buildSearchCriteriaFromDriverQueryParams(queryParameters.getDriverNumber());
        } else if (nonNull(summaryQueryParameters)) {
            searchReason = buildSearchReason(summaryQueryParameters.getReasonType(), summaryQueryParameters.getReference());
            searchCriteria = buildSearchCriteriaFromDriverSummaryParams(summaryQueryParameters);
        }

        final DriverAuditContent driverAuditContent = new DriverAuditContent.Builder()
                .id(UUID.randomUUID())
                .dateTime(ZonedDateTime.now())
                .userId(userDetails.getUserId())
                .userEmail(userDetails.getEmail() != null ? userDetails.getEmail() : EMPTY)
                .searchReason(searchReason)
                .searchCriteria(searchCriteria)
                .build();

        sender.send(envelopeFrom(metadataFrom(metadata)
                .withName(STAGINGDVLA_COMMAND_HANDLER_AUDIT_DRIVER_RECORD)
                .build(), driverAuditContent));
    }

    private SearchCriteria buildSearchCriteriaFromDriverQueryParams(String driverNumber) {
        return SearchCriteria.newBuilder()
                .drivingLicenceNumber(driverNumber)
                .build();
    }

    private SearchReason buildSearchReason(final String reasonType, final String reference) {
        final String trimmedReference = reference.replaceAll("\\s+","").trim();
        return SearchReason.newBuilder()
                .reasonType(DriverAuditTypes.valueOf(reasonType).getReasonType())
                .reference(trimmedReference)
                .build();
    }

    private SearchCriteria buildSearchCriteriaFromDriverSummaryParams(DriverSummaryQueryParameters summaryQueryParameters) {
        return SearchCriteria.newBuilder()
                .firstName(ofNullable(summaryQueryParameters.getFirstNames()).orElse(EMPTY))
                .lastName(ofNullable(summaryQueryParameters.getLastName()).orElse(EMPTY))
                .dateOfBirth(ofNullable(summaryQueryParameters.getDateOfBirth()).orElse(EMPTY))
                .gender(ofNullable(summaryQueryParameters.getGender()).orElse(EMPTY))
                .postcode(ofNullable(summaryQueryParameters.getPostcode()).orElse(EMPTY))
                .build();
    }
}

