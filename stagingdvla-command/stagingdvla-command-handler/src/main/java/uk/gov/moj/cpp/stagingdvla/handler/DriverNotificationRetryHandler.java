package uk.gov.moj.cpp.stagingdvla.handler;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.stagingdvla.handler.util.EventStreamAppender.appendEventsToStream;

import uk.gov.justice.cpp.stagingdvla.command.handler.ScheduleNextRetryForDriverNotified;
import uk.gov.justice.cpp.stagingdvla.command.handler.TriggerNextRetryForDriverNotified;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(COMMAND_HANDLER)
public class DriverNotificationRetryHandler {
    private static final Logger LOGGER = getLogger(DriverNotificationRetryHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("stagingdvla.command.handler.schedule-next-retry-for-driver-notified")
    public void handleScheduleNextRetryForDriverNotified(final Envelope<ScheduleNextRetryForDriverNotified> envelope) throws EventStreamException {
        logRequestReceived("stagingdvla.command.handler.schedule-next-retry-for-driver-notified", envelope);

        final ScheduleNextRetryForDriverNotified payload = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(payload.getMasterDefendantId());
        final DefendantAggregate defendantAggregate = aggregateService.get(eventStream, DefendantAggregate.class);

        final Stream<Object> events = defendantAggregate
                .scheduleNextRetryForDriverNotified(
                        payload.getConvictionId(),
                        payload.getMasterDefendantId());

        if (nonNull(events)) {
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    @Handles("stagingdvla.command.handler.trigger-next-retry-for-driver-notified")
    public void handleTriggerNextRetryForDriverNotified(final Envelope<TriggerNextRetryForDriverNotified> envelope) throws EventStreamException {
        logRequestReceived("stagingdvla.command.handler.trigger-next-retry-for-driver-notified", envelope);

        final TriggerNextRetryForDriverNotified payload = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(payload.getMasterDefendantId());
        final DefendantAggregate defendantAggregate = aggregateService.get(eventStream, DefendantAggregate.class);

        final Stream<Object> events = defendantAggregate
                .triggerNextRetryForDriverNotified(
                        payload.getConvictionId(),
                        payload.getMasterDefendantId());

        if (nonNull(events)) {
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    private void logRequestReceived(final String handlerName, final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received request {} {}", handlerName, envelope.metadata().asJsonObject());
        }
    }
}
