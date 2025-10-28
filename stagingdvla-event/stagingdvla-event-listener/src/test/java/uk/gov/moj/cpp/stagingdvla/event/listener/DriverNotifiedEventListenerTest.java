package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryCancelled;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryScheduled;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.persistence.entity.DrivingConvictionRetryEntity;
import uk.gov.moj.cpp.persistence.repository.DrivingConvictionRetryRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

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
public class DriverNotifiedEventListenerTest {

    private static final UUID CONVICTION_ID = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final ZonedDateTime CREATED_AT = ZonedDateTime.now();

    @InjectMocks
    private DriverNotifiedEventListener listener;

    @Mock
    private DrivingConvictionRetryRepository convictionRetryRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<DrivingConvictionRetryEntity> retryEntityArgumentCaptor;

    private DrivingConvictionRetryEntity drivingConvictionRetryEntity;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        drivingConvictionRetryEntity = new DrivingConvictionRetryEntity();
        drivingConvictionRetryEntity.setConvictionId(CONVICTION_ID);
        drivingConvictionRetryEntity.setMasterDefendantId(MASTER_DEFENDANT_ID);
        drivingConvictionRetryEntity.setCreatedDateTime(CREATED_AT);
    }

    @Test
    public void shouldRemoveRecord_WhenDriverNotifiedRetryAttempted() {
        when(convictionRetryRepository.findBy(CONVICTION_ID)).thenReturn(drivingConvictionRetryEntity);

        callHandleDriverNotified(CONVICTION_ID, 2);

        verify(this.convictionRetryRepository, times(1)).findBy(CONVICTION_ID);
        verify(this.convictionRetryRepository, times(1)).remove(drivingConvictionRetryEntity);
    }

    @Test
    public void shouldNotRemoveRecord_WhenDriverNotifiedRetryAttemptedButRecordNotFound() {
        when(convictionRetryRepository.findBy(CONVICTION_ID)).thenReturn(null);

        callHandleDriverNotified(CONVICTION_ID, 3);

        verify(this.convictionRetryRepository, times(1)).findBy(CONVICTION_ID);
        verify(this.convictionRetryRepository, times(0)).remove(any());
    }

    @Test
    public void shouldDoNothing_WhenDriverNotifiedFirstAttempt() {

        callHandleDriverNotified(CONVICTION_ID, 0);

        verify(this.convictionRetryRepository, times(0)).findBy(any());
        verify(this.convictionRetryRepository, times(0)).remove(any());
    }

    @Test
    public void shouldDoNothing_WhenDriverNotifiedFirstAttemptAndIdentifierNull() {

        callHandleDriverNotified(null, 0);

        verify(this.convictionRetryRepository, times(0)).findBy(any());
        verify(this.convictionRetryRepository, times(0)).remove(any());
    }

    @Test
    public void shouldSaveRecord_WhenDriverNotifiedNextRetryScheduled() {

        callHandleDriverNotifiedNextRetryScheduled(CONVICTION_ID);

        verify(this.convictionRetryRepository, times(1)).save(retryEntityArgumentCaptor.capture());
        DrivingConvictionRetryEntity entity = retryEntityArgumentCaptor.getAllValues().get(0);
        assertThat(entity.getConvictionId(), equalTo(CONVICTION_ID));
        assertThat(entity.getMasterDefendantId(), equalTo(MASTER_DEFENDANT_ID));
    }

    @Test
    public void shouldNotSaveRecord_WhenDriverNotifiedNextRetryScheduledButConvictionIdIsNull() {

        callHandleDriverNotifiedNextRetryScheduled(null);

        verify(this.convictionRetryRepository, times(0)).save(drivingConvictionRetryEntity);
    }

    @Test
    public void shouldRemoveRecord_WhenDriverNotifiedNextRetryCancelled() {
        when(convictionRetryRepository.findBy(CONVICTION_ID)).thenReturn(drivingConvictionRetryEntity);

        callHandleDriverNotifiedNextRetryCancelled(CONVICTION_ID);

        verify(this.convictionRetryRepository, times(1)).findBy(CONVICTION_ID);
        verify(this.convictionRetryRepository, times(1)).remove(drivingConvictionRetryEntity);
    }

    @Test
    public void shouldNotRemoveRecord_WhenDriverNotifiedNextRetryCancelledButEntityNotFound() {
        when(convictionRetryRepository.findBy(CONVICTION_ID)).thenReturn(null);

        callHandleDriverNotifiedNextRetryCancelled(CONVICTION_ID);

        verify(this.convictionRetryRepository, times(1)).findBy(CONVICTION_ID);
        verify(this.convictionRetryRepository, times(0)).remove(any());
    }

    @Test
    public void shouldNotRemoveRecord_WhenDriverNotifiedNextRetryCancelledButConvictionIdIsNull() {

        callHandleDriverNotifiedNextRetryCancelled(null);

        verify(this.convictionRetryRepository, times(0)).findBy(any());
        verify(this.convictionRetryRepository, times(0)).remove(any());
    }

    private void callHandleDriverNotified(final UUID idenfitier, final Integer retrySeq) {
        final DriverNotified event = DriverNotified.driverNotified()
                .withIdentifier(idenfitier)
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .withRetrySequence(retrySeq)
                .build();

        listener.handleDriverNotified(envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-notified")
                        .createdAt(CREATED_AT),
                objectToJsonObjectConverter.convert(event)));
    }

    private void callHandleDriverNotifiedNextRetryScheduled(final UUID convictionId) {
        final DriverNotifiedNextRetryScheduled event = DriverNotifiedNextRetryScheduled.driverNotifiedNextRetryScheduled()
                .withConvictionId(convictionId)
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .build();

        listener.handleDriverNotifiedNextRetryScheduled(envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-notified-next-retry-scheduled")
                        .createdAt(CREATED_AT),
                objectToJsonObjectConverter.convert(event)));
    }

    private void callHandleDriverNotifiedNextRetryCancelled(final UUID convictionId) {
        final DriverNotifiedNextRetryCancelled event = DriverNotifiedNextRetryCancelled.driverNotifiedNextRetryCancelled()
                .withConvictionId(convictionId)
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .build();

        listener.handleDriverNotifiedNextRetryCancelled(
                envelopeFrom(metadataWithRandomUUID("stagingdvla.event.driver-notified-next-retry-cancelled")
                                .createdAt(CREATED_AT),
                        objectToJsonObjectConverter.convert(event)));
    }
}
