package uk.gov.moj.stagingdvla.it;

import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber.stubFeaturesFor;
import static uk.gov.moj.stagingdvla.stubs.DVLANotificationStub.verifyDVLANotificationCommandInvoked;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.stubGenerateDocument;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.verifyGenerateDocumentStubCommandInvoked;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.stagingdvla.stubs.ProgressionStub.stubProgressionAddCourtDocument;
import static uk.gov.moj.stagingdvla.util.QueueUtil.EventListener;
import static uk.gov.moj.stagingdvla.util.QueueUtil.listenFor;
import static uk.gov.moj.stagingdvla.util.QueueUtil.privateEvents;
import static uk.gov.moj.stagingdvla.util.QueueUtil.retrieveMessage;
import static uk.gov.moj.stagingdvla.util.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.stagingdvla.util.RestHelper.postCommandWithUserId;
import static uk.gov.moj.stagingdvla.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.stagingdvla.util.FileUtil;

import java.io.IOException;
import java.util.UUID;

import javax.jms.MessageConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DvlaNotificationIT extends AbstractIntegrationTest {

    public static final String USER_GROUP = UUID.randomUUID().toString();
    private String hearingId;
    private String defendantId;
    private String caseId;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private final MessageConsumer consumerForDriverNotified = privateEvents.createPrivateConsumer("stagingdvla.event.driver-notified");

    private final String DRIVER_NOTIFICATION_MEDIA_TYPE = "application/vnd.stagingdvla.command.driver-notification+json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD = "stagingdvla.command.driver-notification.json";
    private final String DRIVER_D20REMOVAL_NOTIFICATION_COMMAND_PAYLOAD = "stagingdvla.command.driver-d20removal-notification.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT = "stagingdvla.command.driver-notification-with-convicting-court.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT_MULTIPLE_OFFENCES = "stagingdvla.command.driver-notification-with-convicting-court-multiple-offences.json";
    private final String DRIVER_NO_NOTIFICATION_COMMAND_PAYLOAD = "stagingdvla.command.driver-no-notification.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_NO_ENDORSEMENT = "stagingdvla.command.driver-notification-with-no-endorsement.json";
    private final String DRIVER_NOTIFICATION_NEW_ENDORSEMENT_COMMAND_PAYLOAD = "stagingdvla.driver-notified-new-endorsement.dvla-api.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_1 = "stagingdvla.command.driver-notification-linkedcase-1.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_2 = "stagingdvla.command.driver-notification-linkedcase-2.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_3 = "stagingdvla.command.driver-notification-linkedcase-3.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_GENERATE_D20 = "stagingdvla.command.driver-notification-sjp-generate-d20.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_REMOVE_D20 = "stagingdvla.command.driver-notification-sjp-remove-d20.json";

    private static final String STAGINGDVLA_CONTEXT = "stagingdvla";

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP), "stub-data/usersgroups.get-specific-groups-by-user.json");
        stubDocumentCreate("Dummy");
        stubGenerateDocument("Dummy");
        stubProgressionAddCourtDocument();
        final ImmutableMap<String, Boolean> features = of("driverOut", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);
    }

    @BeforeEach
    public void setup() {
        hearingId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseId = randomUUID().toString();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldSendDvlaNotification() throws IOException {



        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then
        verifyEventIsCreated();
        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

    }

    @Test
    public void shouldNotSendDvlaNotificationWhenNoEndorsement() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_NO_ENDORSEMENT);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then
        verifyEventIsNotCreated();
    }

    @Test
    public void shouldSendDvlaD20RemovalNotification() throws IOException {
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyMaterialCreated();

        final String d20RemovalBody = getPayload(DRIVER_D20REMOVAL_NOTIFICATION_COMMAND_PAYLOAD);
        final Response d20RemovalResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, d20RemovalBody, USER_GROUP);
        assertThat(d20RemovalResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldSendDvlaNotificationWithConvictingCourtCode() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT)
                .replaceAll("%HEARING_ID%", hearingId);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        try (final EventListener driverNotifiedEventListener = listenFor("stagingdvla.event.driver-notified")
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].code", CoreMatchers.is("CA03014"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].startDate", CoreMatchers.is("1996-05-04"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].endDate", CoreMatchers.is("2005-05-05"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].mainOffenceCode", CoreMatchers.is("CA03014"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].convictingCourtCode", CoreMatchers.is("2577"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].convictionDate", CoreMatchers.is("2021-10-04"))))
        ) {
            driverNotifiedEventListener.expectNoneWithin(20000);
        }

        //Then
        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldSendDvlaNotificationWithConvictingCourtCodeForMultipleOffences() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT_MULTIPLE_OFFENCES)
                .replaceAll("%HEARING_ID%", hearingId);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        try (final EventListener driverNotifiedEventListener = listenFor("stagingdvla.event.driver-notified")
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].code", CoreMatchers.is("CA03014"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].mainOffenceCode", CoreMatchers.is("CA03014"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].startDate", CoreMatchers.is("1996-05-04"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].endDate", CoreMatchers.is("2005-05-05"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].convictingCourtCode", CoreMatchers.is("2577"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].convictionDate", CoreMatchers.is("2021-03-10"))))

                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].code", CoreMatchers.is("CA03012"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].mainOffenceCode", CoreMatchers.is("CA03012"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].startDate", CoreMatchers.is("2000-11-12"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].endDate", CoreMatchers.is("2021-11-05"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].convictingCourtCode", CoreMatchers.is("1365"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].convictionDate", CoreMatchers.is("2021-10-04"))))
        ) {
            driverNotifiedEventListener.expectNoneWithin(20000);
        }

        //Then
        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldSendDvlaNotificationForNewEndorsement() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_NEW_ENDORSEMENT_COMMAND_PAYLOAD);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        //Then
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldSendDvlaNotificationForUpdatedCase() throws IOException {
        // Case 1 resulted
        final String body1 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_1);
        final Response writeResponse1 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body1, USER_GROUP);
        assertThat(writeResponse1.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyEventIsCreated();

        // Case 2 resulted
        final String body2 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_2);
        final Response writeResponse2 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body2, USER_GROUP);
        assertThat(writeResponse2.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyEventIsCreated();

        // Case 1 is updated
        final String body3 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_3);

        final Response writeResponse3 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body3, USER_GROUP);

        assertThat(writeResponse3.getStatusCode(), equalTo(SC_ACCEPTED));

        // No removal for case 1
        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);

        assertThat(driverNotified.getCases().size(), equalTo(1));
        assertThat(driverNotified.getCases().get(0).getReference(), equalTo("CASE0000001"));
        assertThat(driverNotified.getRemovedEndorsements().size(), equalTo(0));
        assertThat(driverNotified.getUpdatedEndorsements().size(), equalTo(4));

        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldGenerateAndRemoveD20ForSJPCase() throws IOException {
        // Generate D20 for SJP case
        final String body1 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_GENERATE_D20);
        final Response writeResponse1 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body1, USER_GROUP);

        assertThat(writeResponse1.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified1 = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);

        assertThat(driverNotified1, is(notNullValue()));
        assertThat(driverNotified1.getCases().size(), equalTo(1));
        assertThat(driverNotified1.getCases().get(0).getReference(), equalTo("25JAN000011"));
        assertThat(driverNotified1.getRemovedEndorsements(), is(nullValue()));
        assertThat(driverNotified1.getUpdatedEndorsements(), is(nullValue()));

        // Remove D20 for above SJP case
        final String body2 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_REMOVE_D20);
        final Response writeResponse2 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body2, USER_GROUP);
        assertThat(writeResponse2.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified2 = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);

        assertThat(driverNotified2, is(notNullValue()));
        assertThat(driverNotified2.getCases().size(), equalTo(1));
        assertThat(driverNotified2.getCases().get(0).getReference(), equalTo("25JAN000011"));
        assertThat(driverNotified2.getRemovedEndorsements().size(), equalTo(1));
        assertThat(driverNotified2.getUpdatedEndorsements().size(), equalTo(0));

        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    private String getPayload(String fileName) {
        String body = FileUtil.getPayload(fileName);
        body = body.replaceAll("%HEARING_ID%", hearingId)
                .replaceAll("%CASE_ID%", caseId)
                .replaceAll("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private void verifyEventIsCreated() {
        final JsonPath jsonResponse = retrieveMessage(consumerForDriverNotified);
        assertThat(jsonResponse, is(notNullValue()));
    }

    private void verifyEventIsNotCreated() {
        final JsonPath jsonResponse = retrieveMessage(consumerForDriverNotified);
        assertThat(jsonResponse, is(nullValue()));
    }
}

