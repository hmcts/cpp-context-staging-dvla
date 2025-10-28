package uk.gov.moj.stagingdvla.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.moj.stagingdvla.util.FileUtil.getPayload;

import java.util.UUID;

import org.apache.http.HttpStatus;

public class WireMockStubUtils {

    public static final String MATERIAL_UPLOAD_COMMAND =
            "/material-service/command/api/rest/material/material";

    public static void setupAsAuthorisedUser(final UUID userId, final String responsePayLoad) {
        stubFor(get(urlMatching(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload(responsePayLoad))));
    }

    public static void mockMaterialUpload() {
        stubFor(post(urlMatching(MATERIAL_UPLOAD_COMMAND))
                .willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("")));
    }
}
