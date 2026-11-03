package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus.FAILED;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus.SUCCESS;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotifyGatewayResultServiceTest {

    @Mock
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @InjectMocks
    private NotifyGatewayResultService notifyGatewayResultService;

    @Captor
    private ArgumentCaptor<DocumentDelivery> documentDeliveryCaptor;

    @Captor
    private ArgumentCaptor<Metadata> metadataCaptor;

    private final UUID materialId = randomUUID();

    @Test
    public void shouldRecordEmailStatusSuccessOnNotificationSent() {
        notifyGatewayResultService.handle(sentMessage("STAGINGDVLA_" + materialId));

        assertEmailStatusRecorded(SUCCESS);
    }

    @Test
    public void shouldRecordEmailStatusFailedOnNotificationFailed() {
        notifyGatewayResultService.handle(failedMessage("STAGINGDVLA_" + materialId));

        assertEmailStatusRecorded(FAILED);
    }

    @Test
    public void shouldIgnoreResultForAnotherContext() {
        notifyGatewayResultService.handle(sentMessage("PROGRESSION_" + materialId));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreResultWithoutClientContext() {
        notifyGatewayResultService.handle(sentMessage(null));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldThrowOnUnrecognisedSubject() {
        // the subject is checked before the body is even read, so no body stub is needed here
        final ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(message.getSubject()).thenReturn("notification-bounced");

        assertThrows(MalformedResultMessageException.class, () -> notifyGatewayResultService.handle(message));
        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldThrowOnUnparseableBody() {
        final ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(message.getSubject()).thenReturn("notification-sent");
        when(message.getBody()).thenReturn(BinaryData.fromString("not json"));

        assertThrows(MalformedResultMessageException.class, () -> notifyGatewayResultService.handle(message));
        verifyNoInteractions(documentDeliveryStatusService);
    }

    private void assertEmailStatusRecorded(final DvlaDocumentDeliveryEmailStatus emailStatus) {
        verify(documentDeliveryStatusService).record(metadataCaptor.capture(), documentDeliveryCaptor.capture());
        assertThat(metadataCaptor.getValue(), is(notNullValue()));
        final DocumentDelivery documentDelivery = documentDeliveryCaptor.getValue();
        assertThat(documentDelivery.materialId(), is(materialId));
        assertThat(documentDelivery.emailStatus(), is(emailStatus));
        assertThat(documentDelivery.materialStatus(), is(nullValue()));
        assertThat(documentDelivery.sjpStatus(), is(nullValue()));
    }

    private static ServiceBusReceivedMessage sentMessage(final String clientContext) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("sentTime", "2026-10-22T09:00:00Z");
        if (clientContext != null) {
            payload.add("clientContext", clientContext);
        }
        return messageWithSubjectAndBody("notification-sent", payload.build());
    }

    private static ServiceBusReceivedMessage failedMessage(final String clientContext) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("failedTime", "2026-10-22T09:00:00Z")
                .add("errorMessage", "test failure");
        if (clientContext != null) {
            payload.add("clientContext", clientContext);
        }
        return messageWithSubjectAndBody("notification-failed", payload.build());
    }

    private static ServiceBusReceivedMessage messageWithSubjectAndBody(final String subject, final JsonObject body) {
        final ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(message.getSubject()).thenReturn(subject);
        when(message.getBody()).thenReturn(BinaryData.fromString(body.toString()));
        return message;
    }
}
