package uk.gov.moj.cpp.stagingdvla.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.stagingdvla.processor.helper.EnvelopeHelper.verifySendAtIndex;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.AUDIT_REPORT_ORIGINATOR_VALUE;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.CONTEXT;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.ORIGINATOR_VALUE;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.PROCESS_ID;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.SOURCE;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialAddedProcessorTest {

    @InjectMocks
    private MaterialAddedProcessor materialAddedProcessor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<?>> privateEventCaptor;

    @Captor
    private ArgumentCaptor<Envelope<?>> adminEnvelopeCaptor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessMaterialAddedEvent() {
        final JsonObject metaDataJson = createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(NAME, "material.material-added")
                .add(SOURCE, AUDIT_REPORT_ORIGINATOR_VALUE)
                .add(PROCESS_ID, UUID.randomUUID().toString())
                .add(CONTEXT, createObjectBuilder()
                        .add(USER_ID, UUID.randomUUID().toString()))
                .build();

        MetadataBuilder metadataBuilder = metadataFrom(metaDataJson);

        final JsonEnvelope event = envelopeFrom(
                metadataBuilder.build(),
                createObjectBuilder()
                        .add("materialId", UUID.randomUUID().toString())
                        .build());

        materialAddedProcessor.processEvent(event);
        verify(sender).send(privateEventCaptor.capture());
        final List<Envelope<?>> messageEnvelope = privateEventCaptor.getAllValues();
        assertThat(messageEnvelope, hasSize(1));
        verifySendAtIndex(messageEnvelope, "stagingdvla.command.handler.driver-record-search-audit-report-stored", 0);
    }

    @Test
    public void shouldRecordCompletedAndPendingWhenEmailNotificationRequestSucceeds() {
        final UUID materialId = UUID.randomUUID();
        final JsonEnvelope event = materialAddedEventFor(ORIGINATOR_VALUE, materialId);

        materialAddedProcessor.processEvent(event);

        verify(sender).send(privateEventCaptor.capture());
        verifySendAtIndex(privateEventCaptor.getAllValues(), "stagingdvla.command.send-email-notification", 0);

        verify(sender).sendAsAdmin(adminEnvelopeCaptor.capture());
        final DefaultEnvelope documentDeliveryEnvelope = (DefaultEnvelope) adminEnvelopeCaptor.getValue();
        assertThat(documentDeliveryEnvelope.metadata().name(), is("stagingdvla.command.handler.driver-notification-document-delivery"));
        final JsonObject payload = (JsonObject) documentDeliveryEnvelope.payload();
        assertThat(payload.getString("materialId"), is(materialId.toString()));
        assertThat(payload.getString("materialStatus"), is("SUCCESS"));
    }

    private JsonEnvelope materialAddedEventFor(final String source, final UUID materialId) {
        final JsonObject metaDataJson = createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(NAME, "material.material-added")
                .add(SOURCE, source)
                .add(PROCESS_ID, UUID.randomUUID().toString())
                .add(CONTEXT, createObjectBuilder()
                        .add(USER_ID, UUID.randomUUID().toString()))
                .build();

        final MetadataBuilder metadataBuilder = metadataFrom(metaDataJson);

        return envelopeFrom(
                metadataBuilder.build(),
                createObjectBuilder()
                        .add("materialId", materialId.toString())
                        .build());
    }

}
