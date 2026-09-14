package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;

public class SjpStub {

    public static final String URL_SJP_UPLOAD_CASE_DOCUMENT_COMMAND = "/sjp-service/command/api/rest/sjp/cases/(.*)/upload-case-document/(.*)";
    public static final String SJP_UPLOAD_CASE_DOCUMENT_COMMAND_TYPE = "application/vnd.sjp.upload-case-document+json";

    public static void stubSjpUploadCaseDocument() {
        stubFor(post(urlPathMatching(URL_SJP_UPLOAD_CASE_DOCUMENT_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(SJP_UPLOAD_CASE_DOCUMENT_COMMAND_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())));
    }
}
