package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.stagingdvla.util.FileUtil.getPayload;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class ApimStub {

    public static final String APIM_ENDPOINT = "/dvla/enquiry/v1/drivers/find";
    public static final String APIM_DRIVER_CONVICTION_NOTIFIED_ENDPOINT = "/dvla/enquiry/v1/notify-driving-conviction";

    public static final String APIM_IMAGE_ENDPOINT = "/dvla/enquiry/v1/images/retrieve";

    public static void stubApimDvla() {

        final String body = getPayload("stub-data/driverSummaryResponse.json");
        final String driverImagePayload = getPayload("stub-data/driverImageResponse.json");

        stubFor(post(urlPathMatching(APIM_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader("x-total-count", "3")
                        .withBody(body)));

        stubFor(post(urlPathMatching(APIM_IMAGE_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader("x-total-count", "3")
                        .withBody(driverImagePayload)));

        stubFor(post(urlPathMatching(APIM_DRIVER_CONVICTION_NOTIFIED_ENDPOINT))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void verifyQueryDrivingLicencesWithDefendantInformation(final String payload){
        await().atMost(30, SECONDS).pollDelay(5, SECONDS).pollInterval(5, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(APIM_ENDPOINT));
            requestPatternBuilder.withRequestBody(equalToJson(payload));
            verify(1, requestPatternBuilder);
            return true;
        });
    }
}
