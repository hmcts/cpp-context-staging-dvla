package uk.gov.moj.cpp.stagingdvla.query.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.query.api.service.UserDetails;
import uk.gov.moj.cpp.stagingdvla.query.api.service.UserGroupService;
import uk.gov.moj.cpp.stagingdvla.query.view.StagingdvlaQueryView;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverAuditQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverSummaryQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Address;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Disqualification;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Driver;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverSummary;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverSummaryResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Endorsement;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Entitlement;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Intoxicant;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Licence;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Markers;
import uk.gov.moj.cpp.stagingdvla.query.view.response.PreviousDrivingLicence;
import uk.gov.moj.cpp.stagingdvla.query.view.response.PrisonSentenceSuspendedPeriod;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Restriction;
import uk.gov.moj.cpp.stagingdvla.query.view.response.TestPass;
import uk.gov.moj.cpp.stagingdvla.query.view.response.TokenValidity;
import uk.gov.moj.cpp.stagingdvla.query.view.response.UnstructuredAddress;
import uk.gov.moj.cpp.stagingdvla.query.view.service.DriverService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingdvlaQueryApiTest {

    private static final String DRIVER_NUMBER = "MORGA657054SM9IJ";
    private static final String CASE_ID = UUID.randomUUID().toString();
    private static final String LAST_NAME = "Harrison";
    private static final String POSTCODE = "SW1 4DJ";
    private static final String REFERENCE = " CASE- 12345 ";

    @Mock
    private DriverService service;

    @Mock
    StagingdvlaQueryView stagingdvlaQueryView;

    @InjectMocks
    private StagingdvlaQueryApi stagingdvlaQueryApi;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private UserGroupService userGroupService;

    @Mock
    private Sender sender;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    void shouldReturnDriver() {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID)
                .reasonType("FPE")
                .reference(REFERENCE)
                .build();
        final DriverResponse driverResponse = prepareDriverResponse(DRIVER_NUMBER);
        final Envelope<DriverQueryParameters> envelope = envelopeFrom(metadataWithDefaults()
                .withName("stagingdvla.query.drivernumber"), queryParameters);

        when(service.findByDriverNumber(queryParameters)).thenReturn(driverResponse);

        when(userGroupService.getUserById(envelope.metadata())).thenReturn(getUserDetails());
        final Envelope<DriverResponse> driverResponseEnvelope = stagingdvlaQueryApi.findByDriverNumber(envelope);

        assertThat(driverResponseEnvelope.metadata(), withMetadataEnvelopedFrom(envelope));
        assertThat(driverResponseEnvelope.payload(), is(driverResponse));
    }

    @Test
    void shouldReturnBadRequestForInvalidDriverNumber() {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER+"1234")
                .caseId(CASE_ID)
                .reasonType("FPE")
                .reference(REFERENCE)
                .build();
        final DriverResponse driverResponse = prepareDriverResponse(DRIVER_NUMBER);
        final Envelope<DriverQueryParameters> envelope = envelopeFrom(metadataWithDefaults()
                .withName("stagingdvla.query.drivernumber"), queryParameters);


        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () ->  stagingdvlaQueryApi.findByDriverNumber(envelope)
        );

    }


    @Test
    void shouldReturnDriverList() {

        final DriverSummaryQueryParameters queryParameters = new DriverSummaryQueryParameters.Builder()
                .lastName(LAST_NAME)
                .postcode(POSTCODE)
                .reasonType("CE")
                .reference(REFERENCE)
                .dateOfBirth("1971-06-22")
                .build();
        final DriverSummaryResponse driverSummaryResponse = prepareDriverSummaryResponse(LAST_NAME, POSTCODE);
        final Envelope<DriverSummaryQueryParameters> envelope = envelopeFrom(metadataWithDefaults()
                .withName("stagingdvla.query.driverdetails"), queryParameters);
        when(userGroupService.getUserById(envelope.metadata())).thenReturn(getUserDetails());
        when(service.findByDriverDetails(queryParameters)).thenReturn(driverSummaryResponse);

        final Envelope<DriverSummaryResponse> driverSummaryResponseEnvelope = stagingdvlaQueryApi.findByDriverDetails(envelope);

        assertThat(driverSummaryResponseEnvelope.metadata(), withMetadataEnvelopedFrom(envelope));
        assertThat(driverSummaryResponseEnvelope.payload(), is(driverSummaryResponse));
    }

    @Test
    void shouldThrowBadRequestForInvalidDOB() {

        final DriverSummaryQueryParameters queryParameters = new DriverSummaryQueryParameters.Builder()
                .lastName(LAST_NAME)
                .postcode(POSTCODE)
                .reasonType("CE")
                .reference(REFERENCE)
                .dateOfBirth("81-07-31")
                .build();
        final DriverSummaryResponse driverSummaryResponse = prepareDriverSummaryResponse(LAST_NAME, POSTCODE);
        final Envelope<DriverSummaryQueryParameters> envelope = envelopeFrom(metadataWithDefaults()
                .withName("stagingdvla.query.driverdetails"), queryParameters);


        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () ->  stagingdvlaQueryApi.findByDriverDetails(envelope)
        );
    }

    @Test
    void shouldThrowBadRequestForInvalidPostCode() {

        final DriverSummaryQueryParameters queryParameters = new DriverSummaryQueryParameters.Builder()
                .lastName(LAST_NAME)
                .postcode("AB24 3QB,")
                .reasonType("CE")
                .reference(REFERENCE)
                .build();
        final DriverSummaryResponse driverSummaryResponse = prepareDriverSummaryResponse(LAST_NAME, POSTCODE);
        final Envelope<DriverSummaryQueryParameters> envelope = envelopeFrom(metadataWithDefaults()
                .withName("stagingdvla.query.driverdetails"), queryParameters);


        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () ->  stagingdvlaQueryApi.findByDriverDetails(envelope)
        );
    }

    @Test
    void driverAuditWithNoResponseFromDvla() {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID)
                .reasonType("FPE")
                .reference(REFERENCE)
                .build();

        final Envelope<DriverQueryParameters> envelope = envelopeFrom(metadataWithDefaults()
                .withName("stagingdvla.query.drivernumber"), queryParameters);
        final DriverResponse driverResponse = prepareDriverResponse(DRIVER_NUMBER);
        when(service.findByDriverNumber(queryParameters)).thenReturn(driverResponse);

        when(userGroupService.getUserById(envelope.metadata())).thenReturn(getUserDetails());
        final Envelope<DriverResponse> driverResponseEnvelope = stagingdvlaQueryApi.findByDriverNumber(envelope);

        assertThat(driverResponseEnvelope.metadata(), withMetadataEnvelopedFrom(envelope));
        assertThat(driverResponseEnvelope.payload(), is(driverResponse));
    }

    @Test
    void shouldReturnNotifyDrivingConvictionRetryRecords() {
        stagingdvlaQueryApi.findDrivingConvictionRetries(jsonEnvelope);
        verify(stagingdvlaQueryView).findDrivingConvictionRetries(jsonEnvelope);
    }

    @Test
    void shouldPassEnvelopeThroughToQueryView() {
        final Envelope<DriverAuditQueryParameters> envelope = mock(Envelope.class);
        final JsonEnvelope response = mock(JsonEnvelope.class);
        when(stagingdvlaQueryApi.findDriverAuditRecords(envelope)).thenReturn(response);
        MatcherAssert.assertThat(stagingdvlaQueryApi.findDriverAuditRecords(envelope), is(response));
    }

    private DriverResponse prepareDriverResponse(String drivingLicenceNumber) {
        return new DriverResponse.Builder()
                .driver(new Driver.Builder()
                        .drivingLicenceNumber(drivingLicenceNumber)
                        .firstNames("Walter")
                        .lastName("Black")
                        .title("Mr")
                        .nameFormat("Name LastName")
                        .fullModeOfAddress("15 Live Street, Kensington, London, UK ")
                        .gender("male")
                        .dateOfBirth(LocalDate.of(1958, 4, 25))
                        .placeOfBirth("Taiwan")
                        .address(new Address.Builder().unstructuredAddress(new UnstructuredAddress.Builder()
                                .line1("Apartment No 30")
                                .line2("Live Street")
                                .line3("line 3")
                                .line4("London")
                                .line5("UK")
                                .postcode("S15 93U").build()).build())
                        .disqualifiedUntil(LocalDate.of(2021, 10, 30))
                        .disqualifiedForLife(false)
                        .eyesight("excellent eyesight")
                        .imagesExist(true)
                        .disqualifiedPendingSentence(true)
                        .retainedC1_d1Entitlement(true)
                        .previousDrivingLicence(Stream.of(new PreviousDrivingLicence.Builder()
                                        .previousDrivingLicenceNumber("DV12341234AU")
                                        .previousFirstNames("Jimmy")
                                        .previousLastName("Heisenberg").build(), new PreviousDrivingLicence.Builder()
                                        .previousDrivingLicenceNumber("CV99999999AA")
                                        .previousFirstNames("Matt")
                                        .previousLastName("Eye").build())
                                .collect(Collectors.toList()))
                        .build())
                .endorsements(Stream.of(
                                new Endorsement.Builder()
                                        .offenceCode("Off code 12")
                                        .offenceLegalLiteral("Off leg lit")
                                        .offenceDate(LocalDate.of(2020, 10, 20))
                                        .sentenceDate(LocalDate.of(2020, 11, 20))
                                        .penaltyPoints(20)
                                        .fine(BigDecimal.TEN)
                                        .disqualification(new Disqualification.Builder()
                                                .type("Type 1")
                                                .forLife(true)
                                                .years(2020)
                                                .months(10)
                                                .days(15)
                                                .build())
                                        .expiryDate(LocalDate.of(2022, 11, 20))
                                        .appealCourtCode("111")
                                        .appealDate(LocalDate.of(2020, 11, 10))
                                        .convictionDate(LocalDate.of(2015, 11, 10))
                                        .convictionCourtCode("convictionCourtCode1")
                                        .disqualification(new Disqualification.Builder()
                                                .type("Type 2")
                                                .forLife(true)
                                                .years(2010)
                                                .months(11)
                                                .days(15)
                                                .build())
                                        .disqualificationSuspendedPendingAppealDate(LocalDate.of(2019, 11, 10))
                                        .disqualificationReimposedDate(LocalDate.of(2018, 9, 30))
                                        .disqualificationRemovalDate(LocalDate.of(2018, 9, 30))
                                        .disqualifiedPendingSentence("PendingSentence")
                                        .expiryDate(LocalDate.of(2019, 5, 20))
                                        .fine(BigDecimal.ONE)
                                        .fromDate(LocalDate.of(2017, 9, 30))
                                        .identifier("ID 1")
                                        .intoxicant(new Intoxicant.Builder()
                                                .intoxicantType("intoxicant Type")
                                                .testingMethod("testing Method")
                                                .level(1)
                                                .unitType("Millilitres")
                                                .build())
                                        .markers(new Markers.Builder()
                                                .declaredHardship(true)
                                                .build())
                                        .nextReportDate(LocalDate.of(2022, 9, 16))
                                        .notificationSource("my notification source")
                                        .offenceCode("offence code 1")
                                        .offenceLegalLiteral("offence Legal Literal")
                                        .offenceDate(LocalDate.of(2014, 9, 16))
                                        .otherSentence("other sent")
                                        .penaltyPoints(22)
                                        .prisonSentenceSuspendedPeriod(new PrisonSentenceSuspendedPeriod.Builder()
                                                .years(2020)
                                                .months(1)
                                                .days(12)
                                                .hours(2200)
                                                .build())
                                        .rehabilitationCourseCompleted(true)
                                        .sentenceDate(LocalDate.of(2016, 4, 1))
                                        .sentencingCourtCode("London Court 1122")
                                        .build(),
                                new Endorsement.Builder()
                                        .offenceCode("Off code 22")
                                        .offenceLegalLiteral("leg lit")
                                        .offenceDate(LocalDate.of(2000, 10, 20))
                                        .sentenceDate(LocalDate.of(2001, 11, 20))
                                        .penaltyPoints(10)
                                        .fine(BigDecimal.TEN)
                                        .disqualification(new Disqualification.Builder()
                                                .type("Type 22")
                                                .forLife(true)
                                                .years(2000)
                                                .months(15)
                                                .days(15)
                                                .build())
                                        .expiryDate(LocalDate.of(2002, 11, 2))
                                        .appealCourtCode("111")
                                        .appealDate(LocalDate.of(2000, 11, 10))
                                        .convictionDate(LocalDate.of(2005, 10, 1))
                                        .convictionCourtCode("convictionCourtCode1")
                                        .disqualification(new Disqualification.Builder()
                                                .type("Type 2")
                                                .forLife(true)
                                                .years(2001)
                                                .months(1)
                                                .days(15)
                                                .build())
                                        .disqualificationSuspendedPendingAppealDate(LocalDate.of(2011, 11, 1))
                                        .disqualificationReimposedDate(LocalDate.of(2012, 9, 3))
                                        .disqualificationRemovalDate(LocalDate.of(2013, 9, 3))
                                        .disqualifiedPendingSentence("Pending")
                                        .expiryDate(LocalDate.of(2009, 5, 22))
                                        .fine(BigDecimal.ONE)
                                        .fromDate(LocalDate.of(2007, 9, 25))
                                        .identifier("ID 1")
                                        .intoxicant(new Intoxicant.Builder()
                                                .intoxicantType("intoxicant Type 11")
                                                .testingMethod("testing Method 22")
                                                .level(1)
                                                .unitType("Millilitres")
                                                .build())
                                        .markers(new Markers.Builder()
                                                .declaredHardship(true)
                                                .build())
                                        .nextReportDate(LocalDate.of(2022, 9, 16))
                                        .notificationSource("your notification source")
                                        .offenceCode("offence 555")
                                        .offenceLegalLiteral("Offence Legal Lit")
                                        .offenceDate(LocalDate.of(2004, 9, 16))
                                        .otherSentence("other sent")
                                        .penaltyPoints(22)
                                        .prisonSentenceSuspendedPeriod(new PrisonSentenceSuspendedPeriod.Builder()
                                                .years(2010)
                                                .months(1)
                                                .days(10)
                                                .hours(2200)
                                                .build())
                                        .rehabilitationCourseCompleted(true)
                                        .sentenceDate(LocalDate.of(2006, 4, 1))
                                        .sentencingCourtCode("London Court 3322")
                                        .build())
                        .collect(Collectors.toList()))
                .entitlement(Stream.of(
                        new Entitlement.Builder()
                                .categoryCode("Category 1")
                                .categoryLegalLiteral("Category Legal Literal 1")
                                .categoryShortLiteral("Category Short Literal 1")
                                .categoryType("Category Type Example")
                                .fromDate(LocalDate.of(2019, 12, 31))
                                .expiryDate(LocalDate.of(2022, 12, 31))
                                .categoryStatus("Status 1")
                                .restrictions(Stream.of(new Restriction.Builder()
                                                .restrictionCode("Code 123")
                                                .restrictionLiteral("Restriction Lit")
                                                .build(), new Restriction.Builder()
                                                .restrictionCode("Code 12")
                                                .restrictionLiteral("Restriction Lit 2")
                                                .build())
                                        .collect(Collectors.toList()))
                                .restrictedToAutomaticTransmission(true)
                                .fromNonGB(false)
                                .build(),
                        new Entitlement.Builder()
                                .categoryCode("Category 2")
                                .categoryLegalLiteral("Category Legal Literal 2")
                                .categoryShortLiteral("Category Short Literal 2")
                                .categoryType("Cat Type Example")
                                .fromDate(LocalDate.of(2010, 12, 31))
                                .expiryDate(LocalDate.of(2025, 12, 31))
                                .categoryStatus("Status 99")
                                .restrictions(Stream.of(new Restriction.Builder()
                                                .restrictionCode("Code 444")
                                                .restrictionLiteral("Rest Lit")
                                                .build(), new Restriction.Builder()
                                                .restrictionCode("Code 999")
                                                .restrictionLiteral("Rest Lit")
                                                .build())
                                        .collect(Collectors.toList()))
                                .restrictedToAutomaticTransmission(true)
                                .fromNonGB(false)
                                .build()
                ).collect(Collectors.toList()))
                .licence(new Licence.Builder()
                        .type("Type B")
                        .status("Active Status")
                        .statusQualifier("Stat Qualifier 1")
                        .countryToWhichExchanged("Korea")
                        .build())
                .testPass(Stream.of(
                        new TestPass.Builder()
                                .type("Yearly")
                                .categoryCode("Cat1")
                                .categoryLegalLiteral("CatLegLiteral1")
                                .categoryShortLiteral("CatShortLiteral1")
                                .testDate(LocalDate.of(2020, 12, 10))
                                .status("Valid")
                                .withAutomaticTransmission(true)
                                .vehicleAdaptations(Stream.of("vehicleAdapt1", "vehicleAdapt2").collect(Collectors.toList()))
                                .withTrailer(true)
                                .extendedTest(true)
                                .licenceSurrendered(false)
                                .testingAuthority("London Test Authority")
                                .build(),
                        new TestPass.Builder()
                                .type("Monthly")
                                .categoryCode("Cat2")
                                .categoryLegalLiteral("CatLegLiteral2")
                                .categoryShortLiteral("CatShortLiteral2")
                                .testDate(LocalDate.of(2020, 11, 10))
                                .status("InValid")
                                .withAutomaticTransmission(false)
                                .vehicleAdaptations(Stream.of("vehicle Adapt 5", "vehicle Adapt 5").collect(Collectors.toList()))
                                .withTrailer(true)
                                .extendedTest(false)
                                .licenceSurrendered(false)
                                .testingAuthority("Manchester Test Authority")
                                .build()
                ).collect(Collectors.toList()))
                .tokenValidity(new TokenValidity.Builder()
                        .tokenValidFromDate(LocalDate.of(2018, 10, 15))
                        .tokenValidToDate(LocalDate.of(2024, 12, 15))
                        .tokenIssueNumber("1234999LL")
                        .build())
                .driverRedirect(true)
                .build();
    }

    private DriverSummaryResponse prepareDriverSummaryResponse(String lastName, String postcode) {

        return new DriverSummaryResponse.Builder()
                .results(Stream.of(
                                new DriverSummary.Builder()
                                        .drivingLicenceNumber("EA123411DRIVINGLICENCE")
                                        .firstNames("Walter")
                                        .lastName(lastName)
                                        .dateOfBirth(LocalDate.of(1971, 6, 22))
                                        .gender("Male")
                                        .postcode(postcode)
                                        .driverRedirect(true)
                                        .build(),
                                new DriverSummary.Builder()
                                        .drivingLicenceNumber("EA123422DRIVINGLICENCE")
                                        .firstNames("Jennifer")
                                        .lastName(lastName)
                                        .dateOfBirth(LocalDate.of(1966, 12, 18))
                                        .gender("Female")
                                        .postcode(postcode)
                                        .build(),
                                new DriverSummary.Builder()
                                        .drivingLicenceNumber("EA123433DRIVINGLICENCE")
                                        .firstNames("Samuel L.")
                                        .lastName(lastName)
                                        .dateOfBirth(LocalDate.of(1964, 11, 11))
                                        .gender("Male")
                                        .postcode(postcode)
                                        .driverRedirect(false)
                                        .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private UserDetails getUserDetails() {
        UserDetails userDetails = new UserDetails();
        userDetails.setUserId(randomUUID());
        userDetails.setEmail("peter.parker@gmail.com");
        return userDetails;
    }
}