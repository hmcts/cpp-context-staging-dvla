package uk.gov.moj.cpp.stagingdvla.handler;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.RecordNowsMaterialRequest;
import uk.gov.justice.core.courts.StagingdvlaSendEmailNotification;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.MaterialAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class MaterialStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialStatusHandler.class);

    public static final String STAGINGDVLA_COMMAND_RECORD_NOWS_MATERIAL_REQUEST = "stagingdvla.command.record-nows-material-request";
    public static final String STAGING_DVLA_COMMAND_SEND_EMAIL_NOTIFICATION = "stagingdvla.command.send-email-notification";
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private Enveloper enveloper;

    @Handles(STAGINGDVLA_COMMAND_RECORD_NOWS_MATERIAL_REQUEST)
    public void recordNowsMaterial(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", STAGINGDVLA_COMMAND_RECORD_NOWS_MATERIAL_REQUEST, envelope.toObfuscatedDebugString());
        }
        final RecordNowsMaterialRequest recordNowsMaterialRequest = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), RecordNowsMaterialRequest.class);
        final EventStream eventStream = eventSource.getStreamById(recordNowsMaterialRequest.getContext().getMaterialId());
        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);
        final Stream<Object> events = materialAggregate.create(recordNowsMaterialRequest.getContext());
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    @Handles(STAGING_DVLA_COMMAND_SEND_EMAIL_NOTIFICATION)
    public void sendEmailNotification(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", STAGING_DVLA_COMMAND_SEND_EMAIL_NOTIFICATION, envelope.toObfuscatedDebugString());
        }
        final StagingdvlaSendEmailNotification stagingdvlaMaterialAdded = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), StagingdvlaSendEmailNotification.class);
        final EventStream eventStream = eventSource.getStreamById(stagingdvlaMaterialAdded.getMaterialId());
        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);
        final Stream<Object> events = materialAggregate.dvlaMaterialAdded();
        if(nonNull(events)) {
            appendEventsToStream(envelope, eventStream, events);
        }
    }

}
