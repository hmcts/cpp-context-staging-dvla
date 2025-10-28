package uk.gov.moj.cpp.stagingdvla.processor;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.stagingdvla.processor.helper.EnvelopeHelper.verifySendAtIndex;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.AUDIT_REPORT_ORIGINATOR_VALUE;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.CONTEXT;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.PROCESS_ID;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.SOURCE;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

@Disabled
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

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessMaterialAddedEvent() {
        final JsonObject metaDataJson = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(NAME, "material.material-added")
                .add(SOURCE, AUDIT_REPORT_ORIGINATOR_VALUE)
                .add(PROCESS_ID, UUID.randomUUID().toString())
                .add(CONTEXT, Json.createObjectBuilder()
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

}
