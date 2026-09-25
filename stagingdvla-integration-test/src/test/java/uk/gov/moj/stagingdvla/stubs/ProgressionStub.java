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
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class ProgressionStub {

    public static final String URL_PROGRESSION_ADD_COURT_DOCUMENT_COMMAND = "/progression-service/command/api/rest/progression/courtdocument/(.*)";
    public static final String PROGRESSION_ADD_COURT_DOCUMENT_COMMAND_TYPE = "application/vnd.progression.add-court-document+json";

    public static void stubProgressionAddCourtDocument() {
        stubFor(post(urlPathMatching(URL_PROGRESSION_ADD_COURT_DOCUMENT_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(PROGRESSION_ADD_COURT_DOCUMENT_COMMAND_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())));
    }

    public static void verifyProgressionAddCourtDocumentInvoked() {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(URL_PROGRESSION_ADD_COURT_DOCUMENT_COMMAND));

            verify(requestPatternBuilder);
            return true;
        });
    }
}
