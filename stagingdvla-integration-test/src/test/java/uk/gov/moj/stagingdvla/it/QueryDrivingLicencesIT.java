package uk.gov.moj.stagingdvla.it;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.stagingdvla.stubs.ApimStub.verifyQueryDrivingLicencesWithDefendantInformation;
import static uk.gov.moj.stagingdvla.util.FileUtil.getPayload;
import static uk.gov.moj.stagingdvla.util.RestHelper.pollForResponse;
import static uk.gov.moj.stagingdvla.util.RestHelper.pollForResponseWithBadRequest;
import static uk.gov.moj.stagingdvla.util.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.stagingdvla.util.StubUtil.stubUsersAndGroupsForUserDetail;
import static uk.gov.moj.stagingdvla.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.time.LocalDate;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QueryDrivingLicencesIT extends AbstractIntegrationTest {

    public static final String USER_ID = UUID.randomUUID().toString();
    private static final String CONTEXT_NAME = "stagingdvla";
    private static final String TABLE = "driver_audit";
    private static final DatabaseCleaner DATABASE_CLEANER = new DatabaseCleaner();

    @BeforeAll
    public static void init() {
    }

    @BeforeEach
    public void setUp() {
        setupLoggedInUsersPermissionQueryStub();
        stubUsersAndGroupsForUserDetail(UUID.fromString(USER_ID));
        setupAsAuthorisedUser(UUID.fromString(USER_ID), "stub-data/usersgroups.get-specific-groups-by-user.json");
    }

    @AfterAll
    public static void destroy() {
       cleanDatabase(TABLE);
    }

    public static void cleanDatabase(final String tableName) {
        DATABASE_CLEANER.cleanEventStoreTables(CONTEXT_NAME);
        DATABASE_CLEANER.cleanStreamStatusTable(CONTEXT_NAME);
        DATABASE_CLEANER.cleanStreamBufferTable(CONTEXT_NAME);
        DATABASE_CLEANER.cleanViewStoreTables(CONTEXT_NAME, tableName);
    }

    @Test
    void shouldReceiveDrivingLicencesWithDefendantInformation() {
        final String response = pollForResponse("/drivers?lastName=Harrison&firstNames=Walter&dateOfBirth=1971-06-22&exactFirstNamesMatch=false&reasonType=CE&reference=CASEURN", "application/vnd.stagingdvla.query.driverdetails+json", USER_ID);

        verifyQueryDrivingLicencesWithDefendantInformation("{\"criteria\":{\"lastName\":\"Harrison\",\"firstNames\":\"Walter\",\"dateOfBirth\":\"1971-06-22\"}}");

        final String  expected = getPayload("driverSummaryResponse.json");
        assertEquals(expected, response, STRICT);
    }

    @Test
    void shouldReceiveDrivingLicencesWithDefendantInformationWithFirstNamesExacts() {
        final String response = pollForResponse("/drivers?lastName=Harrison&firstNames=Walter&dateOfBirth=1971-06-22&exactFirstNamesMatch=true&reasonType=CE&reference=CASEURN", "application/vnd.stagingdvla.query.driverdetails+json", USER_ID);

        verifyQueryDrivingLicencesWithDefendantInformation("{\"options\":{\"firstNamesMatchType\":\"exact\"},\"criteria\":{\"lastName\":\"Harrison\",\"firstNames\":\"Walter\",\"dateOfBirth\":\"1971-06-22\"}}");

        final String  expected = getPayload("driverSummaryResponse.json");
        assertEquals(expected, response, STRICT);
    }

    @Test
    void shouldReceiveBadRequestForInvalidDOB() {
        final String result = pollForResponseWithBadRequest("/drivers?lastName=Harrison&firstNames=Walter&dateOfBirth=81-07-31&exactFirstNamesMatch=true&reasonType=CE&reference=CASEURN", "application/vnd.stagingdvla.query.driverdetails+json", USER_ID);
        assertThat(result, CoreMatchers.is(""));
    }

    @Test
    void shouldReceiveBadRequestForInvalidPostCode() {
        final String result = pollForResponseWithBadRequest("/drivers?lastName=Harrison&firstNames=Walter&dateOfBirth=1971-06-22&exactFirstNamesMatch=true&reasonType=CE&reference=CASEURN&postcode=\"AB24 3QB,\"", "application/vnd.stagingdvla.query.driverdetails+json", USER_ID);
        assertThat(result, CoreMatchers.is(""));
    }

    @Test
    void shouldReceiveBadRequestForInvalidDriverNumber() {
        final String result = pollForResponseWithBadRequest("/driver/AUPSU711267IE9ZKK?reasonType=CE&reference=CASEURN", "application/vnd.stagingdvla.query.drivernumber+json", USER_ID);
        assertThat(result, CoreMatchers.is(""));
    }

    @Test
    void shouldReceiveDriverImage() {
        final String response = pollForResponse("/driver/AUPSU711267IE9ZK/image", "application/vnd.stagingdvla.query.driverimage+json", USER_ID);

        final String expected = getPayload("driverImageResponse.json");
        assertEquals(expected, response, STRICT);
    }
    @Test
    void shouldDriverAuditSearchWithCaseInsensitiveEmailId() {
        final String USER_ID_1 = UUID.randomUUID().toString();
        final String fromDate = LocalDate.now().minusDays(1).toString();
        final String toDate = LocalDate.now().plusDays(2).toString();
        final String email = "Richard.chapman@Acme.com";
        final String url = format("/driver-audit-records?email=%s&startDate=%s&endDate=%s", email, fromDate, toDate);
        setupAsAuthorisedUser(UUID.fromString(USER_ID_1), "stub-data/usersgroups.get-audit-groups-by-user.json");

        final String response = pollForResponse(url, "application/vnd.stagingdvla.query.driver-audit-records+json", USER_ID_1);

        assertThat(response, StringContains.containsString("richard.chapman@acme.com"));
    }
}
