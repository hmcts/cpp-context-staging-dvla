package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static uk.gov.moj.stagingdvla.util.QueueUtil.publicEvents;

import java.util.UUID;

import javax.json.JsonObject;

public class SjpStub {

    public static final String URL_SJP_UPLOAD_CASE_DOCUMENT_COMMAND = "/sjp-service/command/api/rest/sjp/cases/(.*)/upload-case-document/(.*)";
    public static final String SJP_UPLOAD_CASE_DOCUMENT_COMMAND_TYPE = "application/vnd.sjp.upload-case-document+json";
    public static final String CASE_DOCUMENT_ADDED_EVENT = "public.sjp.case-document-added";

    public static void stubSjpUploadCaseDocument() {
        stubFor(post(urlPathMatching(URL_SJP_UPLOAD_CASE_DOCUMENT_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(SJP_UPLOAD_CASE_DOCUMENT_COMMAND_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())));
    }

    // Simulates sjp's CaseDocumentUpdatedProcessor promoting a filed case document: for a
    // blob-addressed document id is the v3 UUID of the blob uri, materialId is sjp's own new
    // material and documentUri echoes the caseDocumentUri stagingdvla uploaded with
    public static void publishCaseDocumentAddedEvent(final String caseId, final String caseDocumentId,
                                                     final String documentUri, final UUID userId) {
        final JsonObject metadata = createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("name", CASE_DOCUMENT_ADDED_EVENT)
                .add("context", createObjectBuilder().add("user", userId.toString()))
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId)
                .add("id", caseDocumentId)
                .add("materialId", UUID.randomUUID().toString())
                .add("documentType", "ELECTRONIC_NOTIFICATIONS")
                .add("documentUri", documentUri)
                .build();

        publicEvents.publish(CASE_DOCUMENT_ADDED_EVENT, metadata, payload);
    }
}
