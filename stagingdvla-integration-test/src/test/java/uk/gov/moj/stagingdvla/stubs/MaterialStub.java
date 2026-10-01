package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.stagingdvla.it.AbstractIntegrationTest.waitForStubToBeReady;
import static uk.gov.moj.stagingdvla.util.QueueUtil.publicEvents;

import java.util.Objects;
import java.util.UUID;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.restassured.path.json.JsonPath;

public class MaterialStub {

    public static final String UPLOAD_MATERIAL_COMMAND = "/material-service/command/api/rest/material/material";
    public static final String MATERIAL_UPLOAD_COMMAND_TYPE = "material.command.upload-file";
    private static final String MATERIAL_ADDED_EVENT = "material.material-added";
    // MaterialAddedProcessor only reacts when metadata.originator == "d20" (MaterialService.
    // ORIGINATOR_VALUE) - the value the real material context echoes back, inherited from the
    // stagingdvla.command.record-nows-material-request that originally asked it to create this material
    public static final String ORIGINATOR = "d20";
    // MaterialService.AUDIT_REPORT_ORIGINATOR_VALUE - the originator value for the driver-search
    // audit-report flow's own material creation, gating MaterialAddedProcessor.
    // handleDriverAuditReportUploadedEvent
    public static final String AUDIT_REPORT_ORIGINATOR = "auditReport";

    public static void stubMaterialUploadFile() {

        stubFor(post(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody("")
                ));

        stubFor(get(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(UPLOAD_MATERIAL_COMMAND, MATERIAL_UPLOAD_COMMAND_TYPE);
    }

    public static void verifyMaterialCreated() {
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(MaterialStub::call);
    }

    private static Boolean call() {
        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathMatching(UPLOAD_MATERIAL_COMMAND));
        verify(requestPatternBuilder);
        return true;
    }

    // The real material context isn't deployed here, so it never raises material.material-added
    // itself - this publishes it in its place, completing the success flow
    // (SystemDocGeneratorEventProcessor's document-available handling asks it to store the
    // generated document via stagingdvla.command.record-nows-material-request; this is its
    // eventual, asynchronous confirmation). userId is echoed back into metadata.context.user,
    // matching the shape MaterialService.createMetadataWithProcessIdAndUserId builds in production.
    public static void publishMaterialAddedEvent(final UUID materialId, final UUID userId) {
        final JsonObject metadata = createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("name", MATERIAL_ADDED_EVENT)
                .add("originator", ORIGINATOR)
                .add("context", createObjectBuilder().add("user", userId.toString()))
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId.toString())
                .build();

        publicEvents.publish(MATERIAL_ADDED_EVENT, metadata, payload);
    }

    // The audit-report variant: MaterialAddedProcessor.handleDriverAuditReportUploadedEvent only
    // reacts when metadata.originator == "auditReport", and reads the report id back from
    // metadata.processId (not the payload) - for the audit-report flow the material shares the
    // report's own id (see AuditReportAggregate.auditReportCreated, which sets
    // materialId = auditReportCreated.getId())
    public static void publishMaterialAddedEventForAuditReport(final UUID materialId, final String userId) {
        final JsonObject metadata = createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("name", MATERIAL_ADDED_EVENT)
                .add("originator", AUDIT_REPORT_ORIGINATOR)
                .add("processId", materialId.toString())
                .add("context", createObjectBuilder().add("user", userId))
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId.toString())
                .build();

        publicEvents.publish(MATERIAL_ADDED_EVENT, metadata, payload);
    }

    // Waits for the material.command.upload-file POST that NowsMaterialStatusEventProcessor or
    // DriverSearchAuditReportEventProcessor sends (via MaterialService.uploadMaterialFromUri) for this
    // materialId, and returns its body so the test can assert on the file reference it carries and on
    // the _metadata MaterialService embeds in it (e.g. _metadata.originator). Matched by materialId
    // rather than "any request", because the WireMock journal is shared by every test and never
    // reset - and unlike verifyMaterialCreated() this only looks at POSTs, so the stub-readiness GET
    // can't satisfy it.
    public static JsonPath awaitMaterialUploadRequest(final String materialId) {
        return await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() ->
                findAll(postRequestedFor(urlPathEqualTo(UPLOAD_MATERIAL_COMMAND))).stream()
                        .map(request -> new JsonPath(request.getBodyAsString()))
                        .filter(body -> materialId.equals(body.getString("materialId")))
                        .findFirst()
                        .orElse(null), Objects::nonNull);
    }
}
