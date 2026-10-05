package uk.gov.moj.cpp.stagingdvla.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus.FAILED;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus.SUCCESS;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationNotifyEventProcessorTest {

    private static final String NOTIFICATION_SENT = "public.notificationnotify.events.notification-sent";
    private static final String NOTIFICATION_FAILED = "public.notificationnotify.events.notification-failed";

    @Mock
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @InjectMocks
    private NotificationNotifyEventProcessor notificationNotifyEventProcessor;

    @Captor
    private ArgumentCaptor<DocumentDelivery> documentDeliveryCaptor;

    private final UUID materialId = randomUUID();

    @Test
    public void shouldRecordEmailStatusSuccessWhenStagingDvlaNotificationIsSent() {
        final JsonEnvelope envelope = sentEvent("STAGINGDVLA_" + materialId);

        notificationNotifyEventProcessor.handleNotificationSent(envelope);

        assertEmailStatusRecorded(envelope, SUCCESS);
    }

    @Test
    public void shouldRecordEmailStatusFailedWhenStagingDvlaNotificationFails() {
        final JsonEnvelope envelope = failedEvent("STAGINGDVLA_" + materialId);

        notificationNotifyEventProcessor.handleNotificationFailed(envelope);

        assertEmailStatusRecorded(envelope, FAILED);
    }

    @Test
    public void shouldIgnoreSentNotificationOfAnotherContext() {
        notificationNotifyEventProcessor.handleNotificationSent(sentEvent("PROGRESSION_" + materialId));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreFailedNotificationOfAnotherContext() {
        notificationNotifyEventProcessor.handleNotificationFailed(failedEvent("PROGRESSION_" + materialId));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreSentNotificationWithoutClientContext() {
        notificationNotifyEventProcessor.handleNotificationSent(sentEvent(null));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreFailedNotificationWithoutClientContext() {
        notificationNotifyEventProcessor.handleNotificationFailed(failedEvent(null));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    private void assertEmailStatusRecorded(final JsonEnvelope envelope, final DvlaDocumentDeliveryEmailStatus emailStatus) {
        verify(documentDeliveryStatusService).record(eq(envelope.metadata()), documentDeliveryCaptor.capture());
        final DocumentDelivery documentDelivery = documentDeliveryCaptor.getValue();
        assertThat(documentDelivery.materialId(), is(materialId));
        assertThat(documentDelivery.emailStatus(), is(emailStatus));
        assertThat(documentDelivery.materialStatus(), is(nullValue()));
        assertThat(documentDelivery.sjpStatus(), is(nullValue()));
    }

    private static JsonEnvelope sentEvent(final String clientContext) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("sentTime", "2026-10-02T09:00:00Z");
        if (clientContext != null) {
            payload.add("clientContext", clientContext);
        }
        return envelopeFrom(metadataWithRandomUUID(NOTIFICATION_SENT), payload.build());
    }

    private static JsonEnvelope failedEvent(final String clientContext) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("failedTime", "2026-10-02T09:00:00Z")
                .add("errorMessage", "test failure")
                .add("statusCode", 400);
        if (clientContext != null) {
            payload.add("clientContext", clientContext);
        }
        return envelopeFrom(metadataWithRandomUUID(NOTIFICATION_FAILED), payload.build());
    }
}
