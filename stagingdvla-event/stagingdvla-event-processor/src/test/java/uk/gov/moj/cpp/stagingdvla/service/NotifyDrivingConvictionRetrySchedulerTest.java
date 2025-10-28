package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.stagingdvla.domain.DrivingConvictionRetry;
import uk.gov.moj.cpp.stagingdvla.notify.azure.DvlaApimConfig;
import uk.gov.moj.cpp.stagingdvla.query.view.StagingdvlaQueryView;
import uk.gov.moj.cpp.stagingdvla.service.scheduler.NotifyDrivingConvictionRetryScheduler;

import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.TimerService;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

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
public class NotifyDrivingConvictionRetrySchedulerTest {

    private static final String STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY = "stagingdvla.query.driving-conviction-retry";
    private static final String STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED = "stagingdvla.command.handler.trigger-next-retry-for-driver-notified";
    private static final String DRIVING_CONVICTION_RETRIES = "drivingConvictionRetries";
    private static final String CONVICTION_ID = "convictionId";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";

    @InjectMocks
    private NotifyDrivingConvictionRetryScheduler notifyDrivingConvictionRetryScheduler;

    @Mock
    private StagingdvlaQueryView stagingdvlaQueryView;

    @Mock
    private DvlaApimConfig dvlaApimConfig;

    @Mock
    private TimerService timerService;

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final ObjectToJsonObjectConverter objectToJsonObjectConverter =
            new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @BeforeEach
    public void init() {
        ReflectionUtil.setField(notifyDrivingConvictionRetryScheduler, "currentStatus",new AtomicReference<>(NotifyDrivingConvictionRetryScheduler.TimerStatus.RUNNING));
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.listToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldRetryOneRecordWhenTimerExpired() {
        ReflectionUtil.setField(notifyDrivingConvictionRetryScheduler, "currentStatus",new AtomicReference<>(NotifyDrivingConvictionRetryScheduler.TimerStatus.RUNNING));

        JsonArrayBuilder arrayBuilder = createArrayBuilder();
        for (int i = 0; i < 1; i++) {
            final DrivingConvictionRetry retryObj = DrivingConvictionRetry.drivingConvictionRetry()
                    .withConvictionId(randomUUID()).withMasterDefendantId(randomUUID()).build();
            JsonObject jsonObject = objectToJsonObjectConverter.convert(retryObj);
            arrayBuilder.add(jsonObject);
            when(jsonObjectToObjectConverter.convert(jsonObject, DrivingConvictionRetry.class)).thenReturn(retryObj);
        }

        Envelope<JsonObject> objectEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY).build(),
                createObjectBuilder().add(DRIVING_CONVICTION_RETRIES, arrayBuilder.build()).build());
        when(stagingdvlaQueryView.findDrivingConvictionRetries(any())).thenReturn(objectEnvelope);

        notifyDrivingConvictionRetryScheduler.expireTimer();

        verify(stagingdvlaQueryView, times(1)).findDrivingConvictionRetries(any());
        verify(sender).send(envelopeArgumentCaptor.capture());
        verifyNoMoreInteractions(sender);

        assertThat(((AtomicReference<NotifyDrivingConvictionRetryScheduler.TimerStatus>) ReflectionUtil.getValueOfField(notifyDrivingConvictionRetryScheduler,"currentStatus", AtomicReference.class)).get(),
                is(NotifyDrivingConvictionRetryScheduler.TimerStatus.RETRYING));

        final Envelope<JsonObject> jsonObjectEnvelope = envelopeArgumentCaptor.getValue();
        assertThat(jsonObjectEnvelope.metadata().name(), is(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED));
        assertNotNull(jsonObjectEnvelope.payload().getString(CONVICTION_ID));
        assertNotNull(jsonObjectEnvelope.payload().getString(MASTER_DEFENDANT_ID));
    }

    @Test
    public void shouldRetryMultipleRecordsAfterFirstRetrySuccess() {
        ReflectionUtil.setField(notifyDrivingConvictionRetryScheduler, "currentStatus",new AtomicReference<>(NotifyDrivingConvictionRetryScheduler.TimerStatus.RETRYING));

        JsonArrayBuilder arrayBuilder = createArrayBuilder();
        for (int i = 0; i < 3; i++) {
            final DrivingConvictionRetry retryObj = DrivingConvictionRetry.drivingConvictionRetry()
                    .withConvictionId(randomUUID()).withMasterDefendantId(randomUUID()).build();
            JsonObject jsonObject = objectToJsonObjectConverter.convert(retryObj);
            arrayBuilder.add(jsonObject);
            when(jsonObjectToObjectConverter.convert(jsonObject, DrivingConvictionRetry.class)).thenReturn(retryObj);
        }

        Envelope<JsonObject> objectEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY).build(),
                createObjectBuilder().add(DRIVING_CONVICTION_RETRIES, arrayBuilder.build()).build());
        when(stagingdvlaQueryView.findDrivingConvictionRetries(any())).thenReturn(objectEnvelope);

        notifyDrivingConvictionRetryScheduler.dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.SUCCESS);

        verify(stagingdvlaQueryView, times(1)).findDrivingConvictionRetries(any());
        verify(sender, times(3)).send(envelopeArgumentCaptor.capture());
        verifyNoMoreInteractions(sender);

        assertThat(((AtomicReference<NotifyDrivingConvictionRetryScheduler.TimerStatus>) ReflectionUtil.getValueOfField(notifyDrivingConvictionRetryScheduler,"currentStatus", AtomicReference.class)).get(),
                is(NotifyDrivingConvictionRetryScheduler.TimerStatus.RUNNING));

        final Envelope<JsonObject> jsonObjectEnvelope1 = envelopeArgumentCaptor.getAllValues().get(0);
        assertThat(jsonObjectEnvelope1.metadata().name(), is(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED));
        assertNotNull(jsonObjectEnvelope1.payload().getString(CONVICTION_ID));
        assertNotNull(jsonObjectEnvelope1.payload().getString(MASTER_DEFENDANT_ID));

        final Envelope<JsonObject> jsonObjectEnvelope2 = envelopeArgumentCaptor.getAllValues().get(1);
        assertThat(jsonObjectEnvelope2.metadata().name(), is(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED));
        assertNotNull(jsonObjectEnvelope2.payload().getString(CONVICTION_ID));
        assertNotNull(jsonObjectEnvelope2.payload().getString(MASTER_DEFENDANT_ID));

        final Envelope<JsonObject> jsonObjectEnvelope3 = envelopeArgumentCaptor.getAllValues().get(2);
        assertThat(jsonObjectEnvelope3.metadata().name(), is(STAGINGDVLA_COMMAND_HANDLER_TRIGGER_NEXT_RETRY_FOR_DRIVER_NOTIFIED));
        assertNotNull(jsonObjectEnvelope3.payload().getString(CONVICTION_ID));
        assertNotNull(jsonObjectEnvelope3.payload().getString(MASTER_DEFENDANT_ID));
    }

    @Test
    public void shouldNotRetryAfterFirstRetryFailWhenInRetryingStatus() {
        ReflectionUtil.setField(notifyDrivingConvictionRetryScheduler, "currentStatus",new AtomicReference<>(NotifyDrivingConvictionRetryScheduler.TimerStatus.RETRYING));

        JsonArrayBuilder arrayBuilder = createArrayBuilder();
        for (int i = 0; i < 3; i++) {
            final DrivingConvictionRetry retryObj = DrivingConvictionRetry.drivingConvictionRetry()
                    .withConvictionId(randomUUID()).withMasterDefendantId(randomUUID()).build();
            JsonObject jsonObject = objectToJsonObjectConverter.convert(retryObj);
            arrayBuilder.add(jsonObject);
        }

        Envelope<JsonObject> objectEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY).build(),
                createObjectBuilder().add(DRIVING_CONVICTION_RETRIES, arrayBuilder.build()).build());
        notifyDrivingConvictionRetryScheduler.dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.FAIL);

        verify(stagingdvlaQueryView, times(0)).findDrivingConvictionRetries(any());
        verify(sender, times(0)).send(envelopeArgumentCaptor.capture());

        assertThat(((AtomicReference<NotifyDrivingConvictionRetryScheduler.TimerStatus>) ReflectionUtil.getValueOfField(notifyDrivingConvictionRetryScheduler,"currentStatus", AtomicReference.class)).get(),
                is(NotifyDrivingConvictionRetryScheduler.TimerStatus.RUNNING));
    }

    @Test
    public void shouldNotRetryOthersWhenNotInRetryingStatus() {
        ReflectionUtil.setField(notifyDrivingConvictionRetryScheduler, "currentStatus",new AtomicReference<>(NotifyDrivingConvictionRetryScheduler.TimerStatus.RUNNING));

        JsonArrayBuilder arrayBuilder = createArrayBuilder();
        for (int i = 0; i < 3; i++) {
            final DrivingConvictionRetry retryObj = DrivingConvictionRetry.drivingConvictionRetry()
                    .withConvictionId(randomUUID()).withMasterDefendantId(randomUUID()).build();
            JsonObject jsonObject = objectToJsonObjectConverter.convert(retryObj);
            arrayBuilder.add(jsonObject);
        }

        Envelope<JsonObject> objectEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY).build(),
                createObjectBuilder().add(DRIVING_CONVICTION_RETRIES, arrayBuilder.build()).build());

        notifyDrivingConvictionRetryScheduler.dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.SUCCESS);

        verify(stagingdvlaQueryView, times(0)).findDrivingConvictionRetries(any());
        verify(sender, times(0)).send(envelopeArgumentCaptor.capture());

        assertThat(((AtomicReference<NotifyDrivingConvictionRetryScheduler.TimerStatus>) ReflectionUtil.getValueOfField(notifyDrivingConvictionRetryScheduler,"currentStatus", AtomicReference.class)).get(),
                is(NotifyDrivingConvictionRetryScheduler.TimerStatus.RUNNING));
    }

}
