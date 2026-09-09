package uk.gov.moj.cpp.stagingdvla.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.stagingdvla.processor.NotificationNotifyEventProcessor.NOTIFICATION_FAILED_EVENT;
import static uk.gov.moj.cpp.stagingdvla.processor.NotificationNotifyEventProcessor.NOTIFICATION_SENT_EVENT;
import static uk.gov.moj.cpp.stagingdvla.processor.NotificationNotifyEventProcessor.STAGINGDVLA_COMMAND_RECORD_EMAIL_DELIVERY_OUTCOME;
import static uk.gov.moj.cpp.stagingdvla.processor.helper.EnvelopeHelper.verifySendAtIndex;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.domain.constants.StagingDvlaClientContext;
import uk.gov.moj.cpp.stagingdvla.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationNotifyEventProcessorTest {

    private static final String OURS = StagingDvlaClientContext.CLIENT_CONTEXT;

    @InjectMocks
    private NotificationNotifyEventProcessor notificationNotifyEventProcessor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Sender sender;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Captor
    private ArgumentCaptor<Envelope<?>> commandCaptor;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldRecordDeliveredOutcomeCorrelatedThroughTheIdMapping() {
        final UUID materialId = randomUUID();
        final UUID notificationId = randomUUID();
        givenMapping(notificationId, materialId);

        notificationNotifyEventProcessor.handleNotificationSent(sentEvent(OURS, notificationId));

        verify(sender).send(commandCaptor.capture());
        final List<Envelope<?>> sent = commandCaptor.getAllValues();
        assertThat(sent, hasSize(1));
        verifySendAtIndex(sent, STAGINGDVLA_COMMAND_RECORD_EMAIL_DELIVERY_OUTCOME, 0);
        final JsonObject payload = payloadOf(sent.get(0));
        assertThat(payload.getJsonString("materialId").getString(), is(materialId.toString()));
        assertThat(payload.getBoolean("delivered"), is(true));
    }

    @Test
    public void shouldRecordFailedOutcomeCarryingTheError() {
        final UUID materialId = randomUUID();
        final UUID notificationId = randomUUID();
        givenMapping(notificationId, materialId);

        notificationNotifyEventProcessor.handleNotificationFailed(
                failedEvent(OURS, notificationId, "Email address is not valid", 400));

        verify(sender).send(commandCaptor.capture());
        final JsonObject payload = payloadOf(commandCaptor.getValue());
        assertThat(payload.getJsonString("materialId").getString(), is(materialId.toString()));
        assertThat(payload.getBoolean("delivered"), is(false));
        assertThat(payload.getJsonString("errorMessage").getString(), is("Email address is not valid"));
        assertThat(payload.getInt("statusCode"), is(400));
    }

    @Test
    public void shouldIgnoreNotificationEventBelongingToAnotherContext() {
        // every notification event on the platform reaches this subscription
        notificationNotifyEventProcessor.handleNotificationSent(sentEvent("correspondence", randomUUID()));

        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    public void shouldIgnoreNotificationEventWithNoClientContext() {
        // clientContext is optional on both events, and notificationnotify's markAsInvalid route
        // omits it entirely
        notificationNotifyEventProcessor.handleNotificationFailed(
                failedEvent(null, randomUUID(), "gateway timeout", 504));

        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    public void shouldIgnoreOurEventWhenTheNotificationIdIsNotAUuid() {
        final JsonObject payload = createObjectBuilder()
                .add("notificationId", "not-a-uuid")
                .add("sentTime", "2026-09-08T08:31:40Z")
                .add("clientContext", OURS)
                .build();

        notificationNotifyEventProcessor.handleNotificationSent(event(NOTIFICATION_SENT_EVENT, payload));

        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    public void shouldIgnoreOurEventWhenNoMaterialIdIsMappedToTheNotification() {
        final UUID notificationId = randomUUID();
        when(systemIdMapperService.getMaterialIdForNotificationId(notificationId.toString()))
                .thenReturn(Optional.empty());

        notificationNotifyEventProcessor.handleNotificationSent(sentEvent(OURS, notificationId));

        verify(sender, never()).send(any(Envelope.class));
    }

    private void givenMapping(final UUID notificationId, final UUID materialId) {
        when(systemIdMapperService.getMaterialIdForNotificationId(notificationId.toString()))
                .thenReturn(Optional.of(new SystemIdMapping(randomUUID(), notificationId.toString(),
                        "STAGINGDVLA_NOTIFICATION_ID", materialId, "MATERIAL_ID", ZonedDateTime.now())));
    }

    private JsonEnvelope sentEvent(final String clientContext, final UUID notificationId) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("sentTime", "2026-09-08T08:31:40Z");
        if (clientContext != null) {
            payload.add("clientContext", clientContext);
        }
        return event(NOTIFICATION_SENT_EVENT, payload.build());
    }

    private JsonEnvelope failedEvent(final String clientContext, final UUID notificationId,
                                     final String errorMessage, final int statusCode) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("failedTime", "2026-09-08T08:31:40Z")
                .add("errorMessage", errorMessage)
                .add("statusCode", statusCode);
        if (clientContext != null) {
            payload.add("clientContext", clientContext);
        }
        return event(NOTIFICATION_FAILED_EVENT, payload.build());
    }

    private JsonEnvelope event(final String eventName, final JsonObject payload) {
        final JsonObject metadata = createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(NAME, eventName)
                .build();
        return envelopeFrom(metadataFrom(metadata).build(), payload);
    }

    private JsonObject payloadOf(final Envelope<?> envelope) {
        return (JsonObject) envelope.payload();
    }
}
