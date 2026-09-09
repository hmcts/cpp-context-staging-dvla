package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.cpp.stagingdvla.event.EmailNotificationDelivered;
import uk.gov.justice.cpp.stagingdvla.event.EmailNotificationDeliveryFailed;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;
import uk.gov.moj.cpp.persistence.repository.DvlaDocumentDeliveryRepository;
import uk.gov.moj.cpp.stagingdvla.domain.constants.EmailDeliveryStatus;

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
public class DvlaDocumentDeliveryEventListenerTest {

    @InjectMocks
    private DvlaDocumentDeliveryEventListener dvlaDocumentDeliveryEventListener;

    @Mock
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<DvlaDocumentDeliveryEntity> entityCaptor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSetEmailStatusToSuccessWhenDelivered() {
        final UUID materialId = randomUUID();
        when(dvlaDocumentDeliveryRepository.findBy(materialId)).thenReturn(pendingRow(materialId));

        dvlaDocumentDeliveryEventListener.handleEmailNotificationDelivered(deliveredEnvelope(materialId));

        verify(dvlaDocumentDeliveryRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEmailStatus(), is(EmailDeliveryStatus.SUCCESS.getStatus()));
    }

    @Test
    public void shouldSetEmailStatusToFailedWhenDeliveryFailed() {
        final UUID materialId = randomUUID();
        when(dvlaDocumentDeliveryRepository.findBy(materialId)).thenReturn(pendingRow(materialId));

        dvlaDocumentDeliveryEventListener.handleEmailNotificationDeliveryFailed(deliveryFailedEnvelope(materialId));

        verify(dvlaDocumentDeliveryRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEmailStatus(), is(EmailDeliveryStatus.FAILED.getStatus()));
    }

    @Test
    public void shouldNotSaveWhenNoDeliveryRowExists() {
        // the expected path until row creation is implemented - must not call save(null)
        final UUID materialId = randomUUID();
        when(dvlaDocumentDeliveryRepository.findBy(materialId)).thenReturn(null);

        dvlaDocumentDeliveryEventListener.handleEmailNotificationDelivered(deliveredEnvelope(materialId));

        verify(dvlaDocumentDeliveryRepository, never()).save(any(DvlaDocumentDeliveryEntity.class));
    }

    @Test
    public void shouldNotOverwriteAnAlreadyResolvedStatusOnRedelivery() {
        final UUID materialId = randomUUID();
        final DvlaDocumentDeliveryEntity alreadyFailed = pendingRow(materialId);
        alreadyFailed.setEmailStatus(EmailDeliveryStatus.FAILED.getStatus());
        when(dvlaDocumentDeliveryRepository.findBy(materialId)).thenReturn(alreadyFailed);

        dvlaDocumentDeliveryEventListener.handleEmailNotificationDelivered(deliveredEnvelope(materialId));

        verify(dvlaDocumentDeliveryRepository, never()).save(any(DvlaDocumentDeliveryEntity.class));
        assertThat(alreadyFailed.getEmailStatus(), is(EmailDeliveryStatus.FAILED.getStatus()));
    }

    private DvlaDocumentDeliveryEntity pendingRow(final UUID materialId) {
        return new DvlaDocumentDeliveryEntity(materialId, ZonedDateTime.now(),
                EmailDeliveryStatus.PENDING.getStatus(), EmailDeliveryStatus.PENDING.getStatus(), null, null);
    }

    private JsonEnvelope deliveredEnvelope(final UUID materialId) {
        final EmailNotificationDelivered event = EmailNotificationDelivered.emailNotificationDelivered()
                .withMaterialId(materialId)
                .withNotificationId(randomUUID())
                .build();
        return envelopeFrom(metadataWithRandomUUID("stagingdvla.event.email-notification-delivered"),
                objectToJsonObjectConverter.convert(event));
    }

    private JsonEnvelope deliveryFailedEnvelope(final UUID materialId) {
        final EmailNotificationDeliveryFailed event = EmailNotificationDeliveryFailed.emailNotificationDeliveryFailed()
                .withMaterialId(materialId)
                .withNotificationId(randomUUID())
                .withErrorMessage("Email address is not valid")
                .withStatusCode(400)
                .build();
        return envelopeFrom(metadataWithRandomUUID("stagingdvla.event.email-notification-delivery-failed"),
                objectToJsonObjectConverter.convert(event));
    }
}
