package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;

import java.util.List;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class DocumentGeneratorStub {

    public static final String PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/render";
    public static final String GENERATE_DOCUMENT_PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/generate-document";

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

}
