package uk.gov.moj.cpp.stagingdvla.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.RecordNowsMaterialRequest;
import uk.gov.justice.core.courts.StagingdvlaSendEmailNotification;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.MaterialAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialStatusHandlerTest {

    @InjectMocks
    private MaterialStatusHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

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
    }

    @Test
    public void shouldHandleRecordNowsMaterial() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("recordNowsMaterial")
                        .thatHandles(MaterialStatusHandler.STAGINGDVLA_COMMAND_RECORD_NOWS_MATERIAL_REQUEST)));
    }

    @Test
    public void shouldProcessDriverNotificationAndRaiseEvent() throws Exception {
        final MaterialAggregate materialAggregate = new MaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);
        final UUID materialId = UUID.randomUUID();
        final RecordNowsMaterialRequest commandObject = RecordNowsMaterialRequest.recordNowsMaterialRequest()
                .withContext(MaterialDetails.materialDetails()
                        .withMaterialId(materialId)
                        .withSecondClassLetter(true)
                        .build()
                ).build();

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(
                MaterialStatusHandler.STAGINGDVLA_COMMAND_RECORD_NOWS_MATERIAL_REQUEST), objectToJsonObjectConverter.convert(commandObject));

        handler.recordNowsMaterial(command);
        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (verify(eventStream)).append(argumentCaptor.capture());
    }

    @Test
    public void shouldProcessSendEmailNotificationAndRaiseEvent() throws Exception {
        final MaterialAggregate materialAggregate = new MaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);
        final UUID materialId = UUID.randomUUID();
        final StagingdvlaSendEmailNotification commandObject = StagingdvlaSendEmailNotification.stagingdvlaSendEmailNotification()
                        .withMaterialId(materialId)
                        .build();
        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(
                MaterialStatusHandler.STAGING_DVLA_COMMAND_SEND_EMAIL_NOTIFICATION), objectToJsonObjectConverter.convert(commandObject));

        handler.sendEmailNotification(command);

        verify(eventStream, times(0)).append(any());
    }
}
