package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.stagingdvla.util.QueueUtil.publicEvents;

import java.util.UUID;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class NotifyStub {

    public static final String NOTIFICATION_NOTIFY_ENDPOINT = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/.*";
    public static final String NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON = "application/vnd.notificationnotify.send-email-notification+json";
    public static final String COMMAND_MEDIA_TYPE = "application/vnd.notificationnotify.email+json";
    private static final String NOTIFICATION_SENT_EVENT = "public.notificationnotify.events.notification-sent";

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

    // the send-email-notification request DriverNotifiedEventProcessor.handleSentEmailNotificationEvent
    // sends for this clientContext (blob-addressed material), with the D20 email's channel fields
    public static void verifyEmailNotificationSent(final String clientContext) {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).untilAsserted(() ->
                verify(emailNotificationRequestFor(clientContext)
                        .withRequestBody(matchingJsonPath("$.templateId"))
                        .withRequestBody(matchingJsonPath("$.sendToAddress"))
                        .withRequestBody(matchingJsonPath("$.materialUrl"))
                        .withRequestBody(matchingJsonPath("$.personalisation.subject"))));
    }

    public static void verifyNoEmailNotificationSent(final String clientContext) {
        verify(0, emailNotificationRequestFor(clientContext));
    }

    private static RequestPatternBuilder emailNotificationRequestFor(final String clientContext) {
        // notificationnotify.send-email-notification reaches notificationnotify's REST command api as
        // its "email" media type, with the notificationId in the path and the rest of the payload as the body
        return postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE))
                .withRequestBody(matchingJsonPath("$.clientContext", equalTo(clientContext)));
    }

    // The real notificationnotify context isn't deployed here, so it never reports the email outcome
    // itself - this publishes its public notification-sent event in its place, echoing back the
    // clientContext the send-email-notification request carried (only ids/times, as in production)
    public static void publishNotificationSentEvent(final String clientContext, final UUID userId) {
        final JsonObject metadata = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("name", NOTIFICATION_SENT_EVENT)
                .add("context", createObjectBuilder().add("user", userId.toString()))
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("sentTime", "2026-10-02T09:00:00Z")
                .add("clientContext", clientContext)
                .build();

        publicEvents.publish(NOTIFICATION_SENT_EVENT, metadata, payload);
    }
}
