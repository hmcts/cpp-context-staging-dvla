package uk.gov.moj.cpp.stagingdvla.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.cpp.stagingdvla.event.DriverAuditRecord;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.DriverAuditAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DriverAuditRecordHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverAuditRecordHandler.class);

    public static final String STAGINGDVLA_COMMAND_HANDLER_AUDIT_DRIVER_RECORD = "stagingdvla.command.handler.audit-driver-record";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles(STAGINGDVLA_COMMAND_HANDLER_AUDIT_DRIVER_RECORD)
    public void handleDriverAuditRecord(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", STAGINGDVLA_COMMAND_HANDLER_AUDIT_DRIVER_RECORD, envelope.toObfuscatedDebugString());
        }
        final DriverAuditRecord driverAuditRecord = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DriverAuditRecord.class);
        final EventStream eventStream = eventSource.getStreamById(driverAuditRecord.getId());
        final DriverAuditAggregate driverAuditAggregate = aggregateService.get(eventStream, DriverAuditAggregate.class);
        final Stream<Object> events = driverAuditAggregate.createAudit(driverAuditRecord);
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
