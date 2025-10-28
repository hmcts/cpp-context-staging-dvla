package uk.gov.moj.cpp.stagingdvla.handler;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.command.handler.DeleteDriverRecordSearchAuditReport;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportStored;
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
import uk.gov.moj.cpp.stagingdvla.aggregate.AuditReportAggregate;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DriverSearchAuditReportHandler {
    public static final String STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT = "stagingdvla.command.handler.generate-driver-record-search-audit-report";
    public static final String STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT = "stagingdvla.command.handler.delete-driver-record-search-audit-report";
    public static final String STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_CREATED = "stagingdvla.command.handler.driver-record-search-audit-report-created";
    public static final String STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_STORED = "stagingdvla.command.handler.driver-record-search-audit-report-stored";

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverSearchAuditReportHandler.class);
    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles(STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT)
    public void handleGenerateDriverSearchAuditReport(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT, envelope.toObfuscatedDebugString());
        }
        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException("No UserId Supplied"));

        final DriverAuditReportSearchCriteria auditReportSearchCriteria = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DriverAuditReportSearchCriteria.class);
        final UUID auditReportId = UUID.randomUUID();
        final EventStream eventStream = eventSource.getStreamById(auditReportId);
        final AuditReportAggregate auditReportAggregate = aggregateService.get(eventStream, AuditReportAggregate.class);
        final Stream<Object> events = auditReportAggregate.generateAuditReport(auditReportId, fromString(userId), ZonedDateTime.now().toString(), auditReportSearchCriteria);
        appendEventsToStream(envelope, eventStream, events);
    }


    @Handles(STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_CREATED)
    public void handleDriverSearchAuditReportCreated(final Envelope<DriverRecordSearchAuditReportCreated> envelope) throws EventStreamException {
        final DriverRecordSearchAuditReportCreated auditReportCreated = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(auditReportCreated.getId());
        final AuditReportAggregate auditReportAggregate = aggregateService.get(eventStream, AuditReportAggregate.class);

        final Stream<Object> events = auditReportAggregate.auditReportCreated(auditReportCreated);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles(STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_STORED)
    public void handleDriverSearchAuditReportStored(final Envelope<DriverRecordSearchAuditReportStored> envelope) throws EventStreamException {
        final DriverRecordSearchAuditReportStored auditReportStored = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(auditReportStored.getId());
        final AuditReportAggregate auditReportAggregate = aggregateService.get(eventStream, AuditReportAggregate.class);

        final Stream<Object> events = auditReportAggregate.auditReportStored(auditReportStored);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles(STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT)
    public void handleDriverSearchAuditReportDeleted(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT, envelope.toObfuscatedDebugString());
        }
        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException("No UserId Supplied"));
        final DeleteDriverRecordSearchAuditReport deleteAuditReport = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DeleteDriverRecordSearchAuditReport.class);

        final EventStream eventStream = eventSource.getStreamById(deleteAuditReport.getReportId());
        final AuditReportAggregate auditReportAggregate = aggregateService.get(eventStream, AuditReportAggregate.class);
        final Stream<Object> events = auditReportAggregate.deleteAuditReport(deleteAuditReport.getReportId(), fromString(userId), deleteAuditReport.getMaterialId());
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
