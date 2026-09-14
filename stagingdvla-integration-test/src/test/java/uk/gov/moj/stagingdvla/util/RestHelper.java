package uk.gov.moj.stagingdvla.util;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import com.jayway.jsonpath.ReadContext;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matcher;

public class RestHelper {

    public static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int PORT = 8080;
    private static final String BASE_URI = "http://" + HOST + ":" + PORT;
    private static final String READ_BASE_URL = "/stagingdvla-query-api/query/api/rest/stagingdvla";
    public static final int TIMEOUT = 30;

    private static final RequestSpecification REQUEST_SPECIFICATION = new RequestSpecBuilder().setBaseUri(BASE_URI).build();

    public static Response postCommandWithUserId(final String uri, final String mediaType,
                                                 final String jsonStringBody, final String userId) throws IOException {
        return given().spec(REQUEST_SPECIFICATION).and().contentType(mediaType).body(jsonStringBody)
                .header(USER_ID, userId).when().post(uri).then()
                .extract().response();
    }

    public static String pollForResponse(final String path, final String mediaType, final String userId) {

        return poll(requestParams(getReadUrl(path), mediaType)
                .withHeader(USER_ID, userId).build())
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK)
                )
                .getPayload();
    }

    /**
     * Polls a query API endpoint until it returns 200 OK AND its JSON payload matches
     * {@code jsonPathMatcher} - use this (rather than the status-only overload) when the query
     * reads a view store that is populated asynchronously by an event listener, so the poll
     * survives the race between event consumption and the read becoming visible.
     */
    public static String pollForResponse(final String path, final String mediaType, final String userId,
                                          final Matcher<? super ReadContext> jsonPathMatcher) {

        return poll(requestParams(getReadUrl(path), mediaType)
                .withHeader(USER_ID, userId).build())
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(jsonPathMatcher)
                )
                .getPayload();
    }

    public static String pollForResponseWithBadRequest(final String path, final String mediaType, final String userId) {

        return poll(requestParams(getReadUrl(path), mediaType)
                .withHeader(USER_ID, userId).build())
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(BAD_REQUEST)
                )
                .getPayload();
    }

    public static String getReadUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }
}
