package uk.gov.moj.cpp.stagingdvla.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.cpp.stagingdvla.command.handler.ScheduleNextRetryForDriverNotified;
import uk.gov.justice.cpp.stagingdvla.command.handler.TriggerNextRetryForDriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryCancelled;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryScheduled;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregate;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DriverNotificationRetryHandlerTest {
    private static final String IS_WAITING_RETRY_TRIGGER = "isWaitingRetryTrigger";
    private static final String PREVIOUS_DRIVER_NOTIFIED = "previousDriverNotified";
    private static final String STAGINGDVLA_COMMAND_HANDLER_SCHEDULE_NEXT_RETRY_FOR_DRIVER_NOTIFIED = "stagingdvla.command.handler.schedule-next-retry-for-driver-notified";
    private static final String STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED = "stagingdvla.command.handler.trigger-next-retry-for-driver-notified";
    private static final String STAGINGDVLA_EVENT_DRIVER_NOTIFIED_NEXT_RETRY_SCHEDULED = "stagingdvla.event.driver-notified-next-retry-scheduled";
    private static final String STAGINGDVLA_EVENT_DRIVER_NOTIFIED_NEXT_RETRY_CANCELLED = "stagingdvla.event.driver-notified-next-retry-cancelled";
    private static final String STAGINGDVLA_EVENT_DRIVER_NOTIFIED = "stagingdvla.event.driver-notified";

    private static final UUID CONVICTION_ID = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();

    @InjectMocks
    private DriverNotificationRetryHandler driverNotificationRetryHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(DriverNotifiedNextRetryScheduled.class, DriverNotifiedNextRetryCancelled.class, DriverNotified.class);

    @Test
    public void shouldHandleScheduleNextRetryForDriverNotified() {
        assertThat(driverNotificationRetryHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleScheduleNextRetryForDriverNotified")
                        .thatHandles(STAGINGDVLA_COMMAND_HANDLER_SCHEDULE_NEXT_RETRY_FOR_DRIVER_NOTIFIED)));
    }

    @Test
    public void shouldHandleTriggerNextRetryForDriverNotified() throws Exception {
        assertThat(driverNotificationRetryHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleTriggerNextRetryForDriverNotified")
                        .thatHandles(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED)));
    }

    @Test
    public void shouldProcessScheduleNextRetryForDriverNotifiedAndRaiseEvent() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);
        final Envelope<ScheduleNextRetryForDriverNotified> envelope = getCommandEnvelope(STAGINGDVLA_COMMAND_HANDLER_SCHEDULE_NEXT_RETRY_FOR_DRIVER_NOTIFIED);
        driverNotificationRetryHandler.handleScheduleNextRetryForDriverNotified(envelope);
        verifyStartEndEventCreated(STAGINGDVLA_EVENT_DRIVER_NOTIFIED_NEXT_RETRY_SCHEDULED);
    }

    @Test
    public void shouldProcessTriggerNextRetryForDriverNotifiedAndRaiseNotifyEvent_WhenPreviousDriverNotifiedExistsWithSameIdentifier() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);
        setField(defendantAggregate, IS_WAITING_RETRY_TRIGGER, true);
        setField(defendantAggregate, PREVIOUS_DRIVER_NOTIFIED, DriverNotified.driverNotified()
                .withIdentifier(CONVICTION_ID)
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .build());

        final Envelope<TriggerNextRetryForDriverNotified> envelope = getCommandEnvelope(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED);
        driverNotificationRetryHandler.handleTriggerNextRetryForDriverNotified(envelope);
        verifyDriverNotifiedEventCreated(STAGINGDVLA_EVENT_DRIVER_NOTIFIED);
    }

    @Test
    public void shouldProcessTriggerNextRetryForDriverNotifiedAndNotRaiseEvent_WhenIsWaitingRetryIsFalse() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);
        setField(defendantAggregate, DefendantAggregate.class.getDeclaredField(IS_WAITING_RETRY_TRIGGER).getName(), false);
        setField(defendantAggregate, DefendantAggregate.class.getDeclaredField(PREVIOUS_DRIVER_NOTIFIED).getName(), null);

        final Envelope<TriggerNextRetryForDriverNotified> envelope = getCommandEnvelope(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED);
        driverNotificationRetryHandler.handleTriggerNextRetryForDriverNotified(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream.collect(Collectors.toList()), is(empty()));
    }

    @Test
    public void shouldProcessTriggerNextRetryForDriverNotifiedAndRaiseCancelEvent_WhenWaitingRetryButPreviousDriverNotifiedIsDifferentThanRetry() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);
        setField(defendantAggregate, IS_WAITING_RETRY_TRIGGER, true);
        setField(defendantAggregate, PREVIOUS_DRIVER_NOTIFIED, DriverNotified.driverNotified()
                .withIdentifier(randomUUID())
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .build());

        final Envelope<TriggerNextRetryForDriverNotified> envelope = getCommandEnvelope(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED);
        driverNotificationRetryHandler.handleTriggerNextRetryForDriverNotified(envelope);
        verifyStartEndEventCreated(STAGINGDVLA_EVENT_DRIVER_NOTIFIED_NEXT_RETRY_CANCELLED);
    }

    @Test
    public void shouldProcessTriggerNextRetryForDriverNotifiedAndRaiseCancelEvent_WhenIsWaitingRetryButPreviousDriverNotifiedIsNull() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);
        setField(defendantAggregate, IS_WAITING_RETRY_TRIGGER, true);
        setField(defendantAggregate, PREVIOUS_DRIVER_NOTIFIED, null);

        final Envelope<TriggerNextRetryForDriverNotified> envelope = getCommandEnvelope(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED);
        driverNotificationRetryHandler.handleTriggerNextRetryForDriverNotified(envelope);
        verifyStartEndEventCreated(STAGINGDVLA_EVENT_DRIVER_NOTIFIED_NEXT_RETRY_CANCELLED);
    }

    private void verifyStartEndEventCreated(final String eventName) throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(eventName),
                        payload().isJson(allOf(
                                withJsonPath("$.convictionId", is(CONVICTION_ID.toString())),
                                withJsonPath("$.masterDefendantId", is(MASTER_DEFENDANT_ID.toString()))
                        ))
                )
        ));
    }

    private void verifyDriverNotifiedEventCreated(final String eventName) throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(eventName),
                        payload().isJson(allOf(
                                withJsonPath("$.identifier", is(CONVICTION_ID.toString())),
                                withJsonPath("$.masterDefendantId", is(MASTER_DEFENDANT_ID.toString()))
                        ))
                )
        ));
    }

    private Envelope getCommandEnvelope(final String commandName) {
        Object commandPayload = null;
        if (commandName.equals(STAGINGDVLA_COMMAND_HANDLER_SCHEDULE_NEXT_RETRY_FOR_DRIVER_NOTIFIED)) {
            commandPayload = ScheduleNextRetryForDriverNotified.scheduleNextRetryForDriverNotified()
                    .withConvictionId(CONVICTION_ID)
                    .withMasterDefendantId(MASTER_DEFENDANT_ID)
                    .build();
        } else if (commandName.equals(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED)) {
            commandPayload = TriggerNextRetryForDriverNotified.triggerNextRetryForDriverNotified()
                    .withConvictionId(CONVICTION_ID)
                    .withMasterDefendantId(MASTER_DEFENDANT_ID)
                    .build();
        }

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(commandPayload)
                .withName(commandName)
                .withMetadataFrom(requestEnvelope);
    }
}
