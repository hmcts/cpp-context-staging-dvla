package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.stagingdvla.util.QueueUtil.publicEvents;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.path.json.JsonPath;

public class DocumentGeneratorStub {

    public static final String PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/render";
    public static final String GENERATE_DOCUMENT_PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/generate-document";
    private static final String DOCUMENT_AVAILABLE_EVENT = "public.systemdocgenerator.events.document-available";
    private static final String DVLA_DOCUMENT_ORDER = "DVLADocumentOrder";

    public static void stubDocumentCreate(String documentText) {
        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.systemdocgenerator.render+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(documentText.getBytes())));
    }

    public static void stubGenerateDocument(String documentText) {
        stubFor(post(urlPathMatching(GENERATE_DOCUMENT_PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.systemdocgenerator.generate-document+json"))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withBody(documentText.getBytes())));
    }

    public static void verifyGenerateDocumentStubCommandInvoked(final List<DriverNotified> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(GENERATE_DOCUMENT_PATH));
            expectedValues.forEach(
                    expectedValue -> verify(requestPatternBuilder)
            );

            return true;
        });
    }

    public static void verifyGenerateDocumentStubCommandInvoked() {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(GENERATE_DOCUMENT_PATH));

            verify(requestPatternBuilder);
            return true;
        });
    }

    // The generate-document request body carries the real payloadFileServiceId/sourceCorrelationId
    // that DocumentGeneratorService actually stored via the (real, undstubbed) file-service —
    // captured here so a manually published document-available event can reference genuine data.
    public static JsonPath latestGenerateDocumentRequest() {
        return latestGenerateDocumentRequests(1).get(0);
    }

    // WireMock's request journal is never reset between test methods, so this only takes the
    // most recent `count` requests (trusting append order) rather than all requests ever recorded -
    // safe as long as it's called right after the `count` document-generation calls this test caused.
    public static List<JsonPath> latestGenerateDocumentRequests(final int count) {
        final List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching(GENERATE_DOCUMENT_PATH)));
        return requests.subList(Math.max(0, requests.size() - count), requests.size()).stream()
                .map(request -> new JsonPath(request.getBodyAsString()))
                .collect(toList());
    }

    // The real systemdocgenerator service isn't deployed here (only its command-api is stubbed
    // above), so it never raises document-available itself — this publishes it in its place.
    // Returns the generated documentFileServiceId so tests can assert on it downstream.
    public static String publishDocumentAvailableEvent(final String payloadFileServiceId, final String sourceCorrelationId) {
        final JsonObject metadata = createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("name", DOCUMENT_AVAILABLE_EVENT)
                .build();

        final String documentFileServiceId = UUID.randomUUID().toString();
        final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
        final JsonObject payload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId)
                .add("sourceCorrelationId", sourceCorrelationId)
                .add("payloadFileServiceId", payloadFileServiceId)
                .add("templateIdentifier", "EDT_DriverOutNotification")
                .add("conversionFormat", "pdf")
                .add("requestedTime", now)
                .add("generatedTime", now)
                .add("generateVersion", 1)
                .build();

        publicEvents.publish(DOCUMENT_AVAILABLE_EVENT, metadata, payload);

        return documentFileServiceId;
    }
}
