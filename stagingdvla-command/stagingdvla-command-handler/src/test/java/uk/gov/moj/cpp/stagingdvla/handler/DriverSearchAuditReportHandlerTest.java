package uk.gov.moj.cpp.stagingdvla.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.stagingdvla.handler.DriverSearchAuditReportHandler.STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT;
import static uk.gov.moj.cpp.stagingdvla.handler.DriverSearchAuditReportHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_CREATED;
import static uk.gov.moj.cpp.stagingdvla.handler.DriverSearchAuditReportHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_STORED;
import static uk.gov.moj.cpp.stagingdvla.handler.DriverSearchAuditReportHandler.STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportStored;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeleted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.moj.cpp.stagingdvla.aggregate.AuditReportAggregate;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DriverSearchAuditReportHandlerTest {

    @InjectMocks
    private DriverSearchAuditReportHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AuditReportAggregate auditReportAggregate;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        auditReportAggregate = new AuditReportAggregate();
        lenient().when(eventSource.getStreamById(any())).thenReturn(eventStream);
        lenient().when(aggregateService.get(eventStream, AuditReportAggregate.class)).thenReturn(auditReportAggregate);
    }

    @Test
    public void shouldHandleGenerateDriverSearchAuditReport() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handleGenerateDriverSearchAuditReport")
                        .thatHandles(STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT)));
    }

    @Test
    public void shouldHandleDriverSearchAuditReportCreated() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handleDriverSearchAuditReportCreated")
                        .thatHandles(STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_CREATED)));
    }

    @Test
    public void shouldHandleDriverSearchAuditReportStored() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handleDriverSearchAuditReportStored")
                        .thatHandles(DriverSearchAuditReportHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_STORED)));
    }

    @Test
    public void shouldHandleDriverSearchAuditReportDeleted() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handleDriverSearchAuditReportDeleted")
                        .thatHandles(STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT)));
    }

    @Test
    public void shouldProcessGenerateDriverSearchAuditReportAndRaiseEvent() throws Exception {
        final DriverAuditReportSearchCriteria reportSearchCriteria = DriverAuditReportSearchCriteria.driverAuditReportSearchCriteria()
                .withStartDate(LocalDate.now().minusDays(1).toString())
                .withEndDate(LocalDate.now().toString())
                .withEmail("peter@gmail.com")
                .withDriverNumber("MORGA657054SM9BF")
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope command = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, objectToJsonObjectConverter.convert(reportSearchCriteria));

        handler.handleGenerateDriverSearchAuditReport(command);
        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (verify(eventStream)).append(argumentCaptor.capture());
    }

    @Test
    public void shouldProcessDriverSearchAuditReportCreatedAndRaiseEvent() throws Exception {
        final DriverRecordSearchAuditReportCreated reportCreated = DriverRecordSearchAuditReportCreated.driverRecordSearchAuditReportCreated()
                .withId(randomUUID())
                .withReportFileId(randomUUID().toString())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_CREATED)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<DriverRecordSearchAuditReportCreated> envelope = Envelope.envelopeFrom(metadata, reportCreated);

        handler.handleDriverSearchAuditReportCreated(envelope);
        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (verify(eventStream)).append(argumentCaptor.capture());
    }


    @Test
    public void shouldProcessDriverSearchAuditReportStoredAndRaiseEvent() throws Exception {
        final DriverRecordSearchAuditReportStored reportStored = DriverRecordSearchAuditReportStored.driverRecordSearchAuditReportStored()
                .withId(randomUUID())
                .withMaterialId(randomUUID())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(STAGINGDVLA_COMMAND_HANDLER_DRIVER_SEARCH_AUDIT_REPORT_STORED)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<DriverRecordSearchAuditReportStored> envelope = Envelope.envelopeFrom(metadata, reportStored);

        handler.handleDriverSearchAuditReportStored(envelope);
        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (verify(eventStream)).append(argumentCaptor.capture());
    }

    @Test
    public void shouldProcessDriverSearchAuditReportDeletedAndRaiseEvent() throws Exception {
        final DriverSearchAuditReportDeleted reportDeleted = DriverSearchAuditReportDeleted.driverSearchAuditReportDeleted()
                .withId(randomUUID())
                .withMaterialId(randomUUID())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope command = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, objectToJsonObjectConverter.convert(reportDeleted));

        handler.handleDriverSearchAuditReportDeleted(command);
        final var argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (verify(eventStream)).append(argumentCaptor.capture());
    }


}
