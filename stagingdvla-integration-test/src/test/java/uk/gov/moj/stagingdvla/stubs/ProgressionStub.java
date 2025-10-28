package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;

public class ProgressionStub {

    public static final String URL_PROGRESSION_ADD_COURT_DOCUMENT_COMMAND = "/progression-service/command/api/rest/progression/courtdocument/(.*)";
    public static final String PROGRESSION_ADD_COURT_DOCUMENT_COMMAND_TYPE = "application/vnd.progression.add-court-document+json";

    public static void stubProgressionAddCourtDocument() {
        stubFor(post(urlPathMatching(URL_PROGRESSION_ADD_COURT_DOCUMENT_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(PROGRESSION_ADD_COURT_DOCUMENT_COMMAND_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())));
    }
}
