package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import java.util.UUID;

public class NotifyStub {

    public static final String NOTIFICATION_NOTIFY_ENDPOINT = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/*";
    public static final String NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON = "application/vnd.notificationnotify.send-email-notification+json";
    public static final String COMMAND_MEDIA_TYPE = "application/vnd.notificationnotify.email+json";

    public static void stubNotifications() {
        stubFor(post(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString()))
        );
        stubFor(post(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }
}
