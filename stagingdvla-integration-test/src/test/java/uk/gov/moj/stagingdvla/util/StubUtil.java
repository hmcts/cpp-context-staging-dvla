package uk.gov.moj.stagingdvla.util;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.stagingdvla.util.FileUtil.getPayload;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.test.utils.core.http.RestPoller;

import java.util.UUID;

import javax.ws.rs.core.Response;

public class StubUtil {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String USERS_GROUPS_URL = "/usersgroups-service/query/api/rest/usersgroups/users/%s/groups";
    private static final String CONTENT_TYPE = "application/json";
    private static final int HTTP_STATUS_OK = 200;

    private static final int PORT = 8080;
    private static final String BASE_URL = "http://"+HOST+":"+PORT;


    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void stubUser(String userId) {
        stubFor(get(urlPathEqualTo(format(USERS_GROUPS_URL, userId)))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));

        pollForResponseAfterUpdateHasTakenEffect(userId);

    }

    private static void pollForResponseAfterUpdateHasTakenEffect(String userId) {

        RestPoller restPoller = RestPoller.poll(requestParams(BASE_URL + format(USERS_GROUPS_URL, userId), CONTENT_TYPE).build());

        restPoller.until(status().is(Response.Status.OK));
    }

    public static void setupLoggedInUsersPermissionQueryStub() {
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.user-permissions.json"))));

    }

    public static void stubUsersAndGroupsForUserDetail(final UUID userId) {
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/"+ userId))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-user-details.json")
                                .replace("USER_ID", userId.toString()))));
    }

}
