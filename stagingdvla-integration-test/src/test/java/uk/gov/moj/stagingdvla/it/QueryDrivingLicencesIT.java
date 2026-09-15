package uk.gov.moj.stagingdvla.it;

import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber.stubFeaturesFor;
import static uk.gov.moj.stagingdvla.stubs.ApimStub.verifyQueryDrivingLicencesWithDefendantInformation;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.latestGenerateDocumentRequest;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.publishDocumentAvailableEvent;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.verifyGenerateDocumentStubCommandInvoked;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.stagingdvla.util.FileUtil.getPayload;
import static uk.gov.moj.stagingdvla.util.RestHelper.pollForResponse;
import static uk.gov.moj.stagingdvla.util.RestHelper.pollForResponseWithBadRequest;
import static uk.gov.moj.stagingdvla.util.RestHelper.postCommandWithUserId;
import static uk.gov.moj.stagingdvla.util.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.stagingdvla.util.StubUtil.stubUsersAndGroupsForUserDetail;
import static uk.gov.moj.stagingdvla.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.common.collect.ImmutableMap;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
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
        assertThat(result, nullValue());
    }

    @Test
    void shouldReceiveBadRequestForInvalidPostCode() {
        final String result = pollForResponseWithBadRequest("/drivers?lastName=Harrison&firstNames=Walter&dateOfBirth=1971-06-22&exactFirstNamesMatch=true&reasonType=CE&reference=CASEURN&postcode=\"AB24 3QB,\"", "application/vnd.stagingdvla.query.driverdetails+json", USER_ID);
        assertThat(result, nullValue());
    }

    @Test
    void shouldReceiveBadRequestForInvalidDriverNumber() {
        final String result = pollForResponseWithBadRequest("/driver/AUPSU711267IE9ZKK?reasonType=CE&reference=CASEURN", "application/vnd.stagingdvla.query.drivernumber+json", USER_ID);
        assertThat(result, nullValue());
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

    @Test
    void shouldGenerateDriverSearchAuditReport() throws IOException {
        // feature toggle must be on: dvlaFileStore=true routes report generation through the real
        // file-service store path (see DriverSearchAuditReportEventProcessor.processDriverSearchAuditReportRequested)
        final ImmutableMap<String, Boolean> features = of("dvlaFileStore", true);
        stubFeaturesFor(CONTEXT_NAME, features);

        final String reference = "AUDITREPORT" + UUID.randomUUID();
        final String startDate = LocalDate.now().minusDays(1).toString();
        final String endDate = LocalDate.now().plusDays(1).toString();
        final String email = "richard.chapman@acme.com";

        //Given: a driver search query, which as a side effect sends stagingdvla.command.handler.audit-driver-record
        // (see StagingdvlaQueryApi.processDriverAuditInformation) and creates a row in the driver_audit table
        pollForResponse("/drivers?lastName=Harrison&firstNames=Walter&dateOfBirth=1971-06-22&exactFirstNamesMatch=false&reasonType=CE&reference=" + reference,
                "application/vnd.stagingdvla.query.driverdetails+json", USER_ID);

        // confirm the audit record has actually landed in driver_audit (the write above is
        // asynchronous) before requesting the report, by polling the read side for our reference
        pollForResponse(format("/driver-audit-records?email=%s&startDate=%s&endDate=%s", email, startDate, endDate),
                "application/vnd.stagingdvla.query.driver-audit-records+json", USER_ID,
                withJsonPath(format("$.driverAuditRecords[?(@.reference=='%s')]", reference)));

        //When: request generation of the audit report covering that record
        final String generateReportBody = format("{\"startDate\":\"%s\",\"endDate\":\"%s\",\"email\":\"%s\"}", startDate, endDate, email);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-record-search-audit-report/generate"),
                "application/vnd.stagingdvla.command.generate-driver-record-search-audit-report+json", generateReportBody, USER_ID);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then: systemdocgenerator is asked to generate the report document, with a real stored payload
        verifyGenerateDocumentStubCommandInvoked();
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        assertThat(generateDocumentRequest.getString("originatingSource"), equalTo("DvlaAuditRecords"));
        assertThat(generateDocumentRequest.getString("templateIdentifier"), equalTo("DvlaAuditRecords"));
        assertThat(generateDocumentRequest.getString("conversionFormat"), equalTo("csv"));
        final String reportId = generateDocumentRequest.getString("sourceCorrelationId");
        assertThat(reportId, is(notNullValue()));
        final String payloadFileServiceId = generateDocumentRequest.getString("payloadFileServiceId");
        assertThat(payloadFileServiceId, is(notNullValue()));

        // simulate systemdocgenerator raising document-available for the report (it isn't locally
        // deployed, so nothing else will) and verify the resulting material gets created
        publishDocumentAvailableEvent(payloadFileServiceId, reportId, "DvlaAuditRecords", "DvlaAuditRecords", "csv");
        verifyMaterialCreated();
    }

    @Test
    void shouldGenerateDriverSearchAuditReportWithAzureBlob() throws IOException {
        // feature toggle off: dvlaFileStore=false routes report generation through the Azure blob
        // fallback path instead of the real file-service store (see
        // DriverSearchAuditReportEventProcessor.processDriverSearchAuditReportRequested and
        // DocumentGeneratorService.uploadDocumentToAzureBlob)
        final ImmutableMap<String, Boolean> features = of("dvlaFileStore", false);
        stubFeaturesFor(CONTEXT_NAME, features);

        final String reference = "AUDITREPORT" + UUID.randomUUID();
        final String startDate = LocalDate.now().minusDays(1).toString();
        final String endDate = LocalDate.now().plusDays(1).toString();
        final String email = "richard.chapman@acme.com";

        //Given: a driver search query, which as a side effect sends stagingdvla.command.handler.audit-driver-record
        // (see StagingdvlaQueryApi.processDriverAuditInformation) and creates a row in the driver_audit table
        pollForResponse("/drivers?lastName=Harrison&firstNames=Walter&dateOfBirth=1971-06-22&exactFirstNamesMatch=false&reasonType=CE&reference=" + reference,
                "application/vnd.stagingdvla.query.driverdetails+json", USER_ID);

        // confirm the audit record has actually landed in driver_audit (the write above is
        // asynchronous) before requesting the report, by polling the read side for our reference
        pollForResponse(format("/driver-audit-records?email=%s&startDate=%s&endDate=%s", email, startDate, endDate),
                "application/vnd.stagingdvla.query.driver-audit-records+json", USER_ID,
                withJsonPath(format("$.driverAuditRecords[?(@.reference=='%s')]", reference)));

        //When: request generation of the audit report covering that record
        final String generateReportBody = format("{\"startDate\":\"%s\",\"endDate\":\"%s\",\"email\":\"%s\"}", startDate, endDate, email);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-record-search-audit-report/generate"),
                "application/vnd.stagingdvla.command.generate-driver-record-search-audit-report+json", generateReportBody, USER_ID);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then: systemdocgenerator is asked to generate the report document, with the payload
        // uploaded to Azure blob storage - so the request carries payloadFileUri/destinationFileUri
        // rather than payloadFileServiceId
        verifyGenerateDocumentStubCommandInvoked();
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        assertThat(generateDocumentRequest.getString("originatingSource"), equalTo("DvlaAuditRecords"));
        assertThat(generateDocumentRequest.getString("templateIdentifier"), equalTo("DvlaAuditRecords"));
        assertThat(generateDocumentRequest.getString("conversionFormat"), equalTo("csv"));
        final String reportId = generateDocumentRequest.getString("sourceCorrelationId");
        assertThat(reportId, is(notNullValue()));
        assertThat(generateDocumentRequest.getString("payloadFileServiceId"), is(nullValue()));
        final String payloadFileUri = generateDocumentRequest.getString("payloadFileUri");
        assertThat(payloadFileUri, is(notNullValue()));
        assertThat(generateDocumentRequest.getString("destinationFileUri"), equalTo(payloadFileUri + ".csv"));

        // verify the metadata written on the blob itself (DocumentGeneratorService.uploadDocumentToAzureBlob),
        // not just that some upload happened
        verifyAuditReportAzureBlobMetadata(UUID.fromString(reportId), payloadFileUri.replaceAll("^.*?(DriverAuditReport)", "$1"));

        // simulate systemdocgenerator raising document-available for the report (it isn't locally
        // deployed, so nothing else will) and verify the resulting material gets created - the
        // audit-report branch of SystemDocGeneratorEventProcessor.handleDocumentAvailable only reads
        // documentFileServiceId/sourceCorrelationId from the payload, so payloadFileUri simply
        // stands in for the (unused in this branch) payloadFileServiceId argument
        publishDocumentAvailableEvent(payloadFileUri, reportId, "DvlaAuditRecords", "DvlaAuditRecords", "csv");
        verifyMaterialCreated();
    }

    // Connects directly to the Azure blob store DocumentGeneratorService.uploadDocumentToAzureBlob
    // itself wrote to, and asserts on the metadata it set on the blob (correlation_id/fileName/
    // conversionFormat/templateName/numberOfPages/fileSize) - see
    // DriverSearchAuditReportEventProcessor.processDriverSearchAuditReportRequested, which for the
    // audit-report flow keys the blob name off the report id rather than a materialId.
    // Reuses DvlaNotificationIT's Azurite connection constants (same emulator/container) - see
    // DvlaNotificationIT.verifyAzureBlobMetadata for the driver-notification equivalent. The SDK's
    // HTTP client lower-cases x-ms-meta-* header names when parsing the response, so although
    // production code sets these keys in camelCase, BlobProperties.getMetadata() here returns them
    // all-lowercase regardless (the portal/UI re-displays them in their original case).
    private void verifyAuditReportAzureBlobMetadata(final UUID reportId, final String fileName) {
        final JdkHttpClientBuilder httpClientBuilder = new JdkHttpClientBuilder()
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofSeconds(30));

        final BlobContainerClient blobContainerClient = new BlobServiceClientBuilder()
                .httpClient(httpClientBuilder.build())
                .connectionString(DvlaNotificationIT.AZURITE_CONNECTION_STRING)
                .buildClient()
                .getBlobContainerClient(DvlaNotificationIT.AZURE_BLOB_CONTAINER_NAME);
        final BlobClient blobClient = blobContainerClient.getBlobClient(DvlaNotificationIT.AZURE_BLOB_PATH_PREFIX + fileName);

        final Map<String, String> metadata = blobClient.getProperties().getMetadata();

        assertThat(metadata.get("correlation_id"), equalTo(reportId.toString()));
        assertThat(metadata.get("filename"), equalTo(fileName+ ".csv"));
        assertThat(metadata.get("conversionformat"), equalTo("CSV"));
        assertThat(metadata.get("templatename"), equalTo("DvlaAuditRecords"));
        assertThat(metadata.get("numberofpages"), equalTo("1"));
        assertThat(metadata.get("filesize"), is(notNullValue()));
    }
}
