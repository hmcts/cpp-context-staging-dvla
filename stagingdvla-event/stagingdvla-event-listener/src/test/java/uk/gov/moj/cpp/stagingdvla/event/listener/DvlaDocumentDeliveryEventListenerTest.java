package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.cpp.stagingdvla.event.DvlaDocumentDeliveryRecorded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;
import uk.gov.moj.cpp.persistence.repository.DvlaDocumentDeliveryRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

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
public class DvlaDocumentDeliveryEventListenerTest {

    private static final UUID MATERIAL_ID = randomUUID();

    @InjectMocks
    private DvlaDocumentDeliveryEventListener dvlaDocumentDeliveryEventListener;

    @Mock
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<DvlaDocumentDeliveryEntity> entityArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldInsertNewRecordAndSetCreatedAt() {
        final JsonEnvelope event = eventFor(recordedWith("PENDING", "payload/blob/uri", "document/blob/uri", null, null, null));

        when(dvlaDocumentDeliveryRepository.findBy(MATERIAL_ID)).thenReturn(null);

        dvlaDocumentDeliveryEventListener.handleDvlaDocumentDeliveryRecorded(event);

        verify(dvlaDocumentDeliveryRepository).findBy(MATERIAL_ID);
        verify(dvlaDocumentDeliveryRepository).save(entityArgumentCaptor.capture());

        final DvlaDocumentDeliveryEntity entity = entityArgumentCaptor.getValue();
        assertThat(entity.getMaterialId(), is(MATERIAL_ID));
        assertThat(entity.getCreatedAt(), is(notNullValue()));
        assertThat(entity.getMaterialStatus(), is("PENDING"));
        assertThat(entity.getPayloadBlobUri(), is("payload/blob/uri"));
        assertThat(entity.getDocumentBlobUri(), is("document/blob/uri"));

        verifyNoMoreInteractions(dvlaDocumentDeliveryRepository);
    }

    @Test
    public void shouldInsertNewRecordWithSjpCaseFields() {
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        final JsonEnvelope event = eventFor(recordedWith(null, null, null, caseId, sjpCorrelationId, "PENDING"));

        when(dvlaDocumentDeliveryRepository.findBy(MATERIAL_ID)).thenReturn(null);

        dvlaDocumentDeliveryEventListener.handleDvlaDocumentDeliveryRecorded(event);

        verify(dvlaDocumentDeliveryRepository).findBy(MATERIAL_ID);
        verify(dvlaDocumentDeliveryRepository).save(entityArgumentCaptor.capture());

        final DvlaDocumentDeliveryEntity entity = entityArgumentCaptor.getValue();
        assertThat(entity.getMaterialId(), is(MATERIAL_ID));
        assertThat(entity.getCaseId(), is(caseId));
        assertThat(entity.getSjpCorrelationId(), is(sjpCorrelationId));
        assertThat(entity.getSjpStatus(), is("PENDING"));

        verifyNoMoreInteractions(dvlaDocumentDeliveryRepository);
    }

    @Test
    public void shouldUpdateOnlyNonNullFieldsAndPreserveExistingValues() {
        final ZonedDateTime existingCreatedAt = now();
        final DvlaDocumentDeliveryEntity existingEntity = new DvlaDocumentDeliveryEntity(
                MATERIAL_ID, existingCreatedAt, "PENDING", "existing/payload/uri", "existing/document/uri",
                randomUUID(), randomUUID(), "PENDING");

        final JsonEnvelope event = eventFor(recordedWith("COMPLETED", null, null, null, null, "COMPLETED"));

        when(dvlaDocumentDeliveryRepository.findBy(MATERIAL_ID)).thenReturn(existingEntity);

        dvlaDocumentDeliveryEventListener.handleDvlaDocumentDeliveryRecorded(event);

        verify(dvlaDocumentDeliveryRepository).findBy(MATERIAL_ID);
        verify(dvlaDocumentDeliveryRepository).save(entityArgumentCaptor.capture());

        final DvlaDocumentDeliveryEntity entity = entityArgumentCaptor.getValue();
        assertThat(entity.getMaterialId(), is(MATERIAL_ID));
        assertThat(entity.getCreatedAt(), is(existingCreatedAt));
        assertThat(entity.getMaterialStatus(), is("COMPLETED"));
        assertThat(entity.getPayloadBlobUri(), is("existing/payload/uri"));
        assertThat(entity.getDocumentBlobUri(), is("existing/document/uri"));
        assertThat(entity.getCaseId(), is(existingEntity.getCaseId()));
        assertThat(entity.getSjpCorrelationId(), is(existingEntity.getSjpCorrelationId()));
        assertThat(entity.getSjpStatus(), is("COMPLETED"));

        verifyNoMoreInteractions(dvlaDocumentDeliveryRepository);
    }

    private DvlaDocumentDeliveryRecorded recordedWith(final String materialStatus,
                                                        final String payloadBlobUri, final String documentBlobUri,
                                                        final UUID caseId, final UUID sjpCorrelationId, final String sjpStatus) {
        return DvlaDocumentDeliveryRecorded
                .dvlaDocumentDeliveryRecorded()
                .withMaterialId(MATERIAL_ID)
                .withMaterialStatus(materialStatus)
                .withPayloadBlobUri(payloadBlobUri)
                .withDocumentBlobUri(documentBlobUri)
                .withCaseId(caseId)
                .withSjpCorrelationId(sjpCorrelationId)
                .withSjpStatus(sjpStatus)
                .build();
    }

    private JsonEnvelope eventFor(final DvlaDocumentDeliveryRecorded recorded) {
        final JsonObject payload = createObjectBuilder()
                .add("materialId", MATERIAL_ID.toString())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.dvla-document-delivery-recorded"),
                payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(),
                DvlaDocumentDeliveryRecorded.class)).thenReturn(recorded);

        return event;
    }
}
