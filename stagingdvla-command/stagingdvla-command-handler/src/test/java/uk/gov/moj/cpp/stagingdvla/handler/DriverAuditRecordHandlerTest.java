package uk.gov.moj.cpp.stagingdvla.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.cpp.stagingdvla.DriverSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.DriverSearchReason;
import uk.gov.justice.cpp.stagingdvla.event.DriverAuditRecord;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.DriverAuditAggregate;

import java.time.ZonedDateTime;
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
public class DriverAuditRecordHandlerTest {

    @InjectMocks
    private DriverAuditRecordHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private DriverAuditAggregate driverAuditAggregate;

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
        driverAuditAggregate = new DriverAuditAggregate();
    }

    @Test
    public void shouldHandleDriverAuditRecord() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handleDriverAuditRecord")
                        .thatHandles(DriverAuditRecordHandler.STAGINGDVLA_COMMAND_HANDLER_AUDIT_DRIVER_RECORD)));
    }

    @Test
    public void shouldProcessDriverAuditRecordAndRaiseEvent() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DriverAuditAggregate.class)).thenReturn(driverAuditAggregate);
        final DriverAuditRecord commandObject = DriverAuditRecord.driverAuditRecord()
                .withId(randomUUID())
                .withDateTime(ZonedDateTime.now().toString())
                .withUserEmail("peter@gmail.com")
                .withSearchReason(DriverSearchReason.driverSearchReason().build())
                .withSearchCriteria(DriverSearchCriteria.driverSearchCriteria().build())
                .build();


        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(
                DriverAuditRecordHandler.STAGINGDVLA_COMMAND_HANDLER_AUDIT_DRIVER_RECORD), objectToJsonObjectConverter.convert(commandObject));

        handler.handleDriverAuditRecord(command);
        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (verify(eventStream)).append(argumentCaptor.capture());
    }
}
