package uk.gov.moj.stagingdvla.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;

import java.util.List;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class DVLANotificationStub {

    public static final String DVLA_NOTIFICATION_COMMAND = "/dvla/enquiry/v1/notify-driving-conviction";

    public static void verifyDVLANotificationCommandInvoked(final List<DriverNotified> expectedValues) {
        verifyDVLANotificationStubCommandInvoked(DVLA_NOTIFICATION_COMMAND, expectedValues);
    }

    public static void verifyDVLANotificationStubCommandInvoked(final String commandEndPoint, final List<DriverNotified> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(commandEndPoint));
            expectedValues.forEach(
                    expectedValue -> verify(requestPatternBuilder)
            );

            return true;
        });
    }

    public static void verifyDVLANotificationCommandInvoked() {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(DVLA_NOTIFICATION_COMMAND));
            verify(requestPatternBuilder);
            return true;
        });
    }
}
