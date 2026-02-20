package uk.gov.moj.stagingdvla.it;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber.stubFeaturesFor;
import static uk.gov.moj.stagingdvla.stubs.ApimStub.stubApimDvla;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.stubGenerateDocument;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.stubMaterialUploadFile;
import static uk.gov.moj.stagingdvla.stubs.NotifyStub.stubNotifications;
import static uk.gov.moj.stagingdvla.stubs.ProgressionStub.stubProgressionAddCourtDocument;
import static uk.gov.moj.stagingdvla.util.QueueUtil.privateEvents;
import static uk.gov.moj.stagingdvla.util.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.stagingdvla.util.RestHelper.postCommandWithUserId;
import static uk.gov.moj.stagingdvla.util.StubUtil.stubUser;
import static uk.gov.moj.stagingdvla.util.WireMockStubUtils.mockMaterialUpload;
import static uk.gov.moj.stagingdvla.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.stagingdvla.util.FileUtil;
import uk.gov.moj.stagingdvla.util.QueueUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class.getName());

    private static final String STAGINGDVLA_CONTEXT = "stagingdvla";
    private static final String DRIVER_NOTIFICATION_MEDIA_TYPE = "application/vnd.stagingdvla.command.driver-notification+json";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String SCHEME = "http";
    private static final String PORT = "8080";
    private static final String BASE_URI = SCHEME + "://" + HOST + ":" + PORT;
    private static final String WRITE_BASE_URL = "/stagingdvla-service/command/api/rest/stagingdvla";
    final Matcher NOT_NULL_VALUE = notNullValue();
    final Matcher NULL_VALUE = nullValue();

    private static final MessageConsumer consumerForDriverNotified = privateEvents.createPrivateConsumer("stagingdvla.event.driver-notified");
    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private static final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private static final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    protected MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    private static String USER_GROUP = UUID.randomUUID().toString();
    private static UUID USER_ID = UUID.randomUUID();
    private static String hearingId;
    private static String defendantId;

    static {
        stubUser(USER_ID.toString());
        stubNotifications();
        stubMaterialUploadFile();
        stubApimDvla();
        defaultStubs();
    }

    public AbstractIntegrationTest() {
        headers.putSingle(HeaderConstants.USER_ID, USER_ID);
    }

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP), "stub-data/usersgroups.get-specific-groups-by-user.json");
        stubDocumentCreate("Dummy");
        stubGenerateDocument("Dummy");
        stubProgressionAddCourtDocument();
    }

    @BeforeEach
    public void setup() {
        clearMessages();

        defendantId = randomUUID().toString();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        final ImmutableMap<String, Boolean> features = of("driverOut", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        LOGGER.info("Running scenario with Defendant Id: " + defendantId);
    }

    public static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Response.Status.OK);
    }

    public static void waitForStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RequestParams requestParams = requestParams(BASE_URI + resource, mediaType).build();

        poll(requestParams)
                .timeout(20L, SECONDS)
                .until(
                        status().is(expectedStatus)
                );
    }

    public static String getWriteUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    private static void defaultStubs() {
        mockMaterialUpload();
    }

    public static List<DriverNotified> sendAndVerifyEvent(final String filePath, final int expectedEventCount) throws IOException {

        final String body = getPayload(filePath);
        final io.restassured.response.Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final List<DriverNotified> driverNotifiedList = new ArrayList<>();

        if (expectedEventCount > 0) {
            for (int i = 0; i < expectedEventCount; i++) {
                Optional<JsonObject> jsonObject = retrieveMessageAsJsonObject(consumerForDriverNotified);

                if (jsonObject.isPresent()) {
                    driverNotifiedList.add(jsonToObjectConverter.convert(jsonObject.get(), DriverNotified.class));
                }
            }

            assertThat(driverNotifiedList.size(), equalTo(expectedEventCount));

            return driverNotifiedList;
        } else {
            Optional<JsonObject> jsonObject = retrieveMessageAsJsonObject(consumerForDriverNotified);
            assertThat(jsonObject.isEmpty(), is(true));
            return driverNotifiedList;
        }
    }

    void verify(final DriverNotified driverNotified,
                final String expectedCaseReference,
                final Matcher<Object> driverNotifiedMatcher,
                final Matcher<Object> updatedMatcher,
                final List<String> updatedEndorsements,
                final Matcher<Object> removedMatcher,
                final List<String> removedEndorsements) {

        assertThat(driverNotified.getCases(), NOT_NULL_VALUE);
        assertThat(driverNotified.getCases().size(), equalTo(1));
        assertThat(driverNotified.getCases().get(0).getReference(), equalTo(expectedCaseReference));

        assertThat(driverNotified, is(driverNotifiedMatcher));
        assertThat(driverNotified.getUpdatedEndorsements(), is(updatedMatcher));
        assertThat(driverNotified.getRemovedEndorsements(), is(removedMatcher));

        if (!isNull(driverNotified.getUpdatedEndorsements())) {
            assertThat(driverNotified.getUpdatedEndorsements(), is(updatedEndorsements));
        }

        if (!isNull(driverNotified.getRemovedEndorsements())) {
            assertThat(driverNotified.getRemovedEndorsements(), is(removedEndorsements));
        }

    }

    private static String getPayload(String fileName) {
        hearingId = randomUUID().toString();

        String body = FileUtil.getPayload(fileName);
        body = body.replaceAll("%HEARING_ID%", hearingId)
                .replaceAll("%DEFENDANT_ID%", defendantId);

        return body;
    }

    private static void clearMessages() {
        QueueUtil.clearMessages(consumerForDriverNotified);
    }

}
