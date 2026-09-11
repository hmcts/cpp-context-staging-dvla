package uk.gov.moj.cpp.stagingdvla.handler;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.stagingdvla.handler.util.EventStreamAppender.appendEventsToStream;

import uk.gov.justice.cpp.stagingdvla.command.handler.DriverNotificationDocumentDelivery;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.MaterialAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(COMMAND_HANDLER)
public class DriverNotificationDocumentDeliveryHandler {

    protected static final String STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY = "stagingdvla.command.handler.driver-notification-document-delivery";

    private static final Logger LOGGER = getLogger(DriverNotificationDocumentDeliveryHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles(STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY)
    public void handleDocumentDelivery(final Envelope<DriverNotificationDocumentDelivery> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received request {} {}", STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY, envelope.metadata().asJsonObject());
        }

        final DriverNotificationDocumentDelivery documentDelivery = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(documentDelivery.getMaterialId());
        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.recordDocumentDelivery(
                documentDelivery.getMaterialId(),
                documentDelivery.getMaterialStatus(),
                documentDelivery.getEmailStatus(),
                documentDelivery.getPayloadBlobUri(),
                documentDelivery.getDocumentBlobUri(),
                documentDelivery.getCaseId(),
                documentDelivery.getSjpCorrelationId(),
                documentDelivery.getSjpStatus());

        if (nonNull(events)) {
            appendEventsToStream(envelope, eventStream, events);
        }

        LOGGER.info("Document delivery recorded for material id: {}", documentDelivery.getMaterialId());
    }
}
