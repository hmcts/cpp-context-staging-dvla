package uk.gov.moj.cpp.stagingdvla.processor;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.service.MaterialService;

import java.util.UUID;

import javax.json.Json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowsMaterialStatusEventProcessorTest {

    @InjectMocks
    private NowsMaterialStatusEventProcessor nowsMaterialStatusEventProcessor;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private MaterialService materialService;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessRequestRecorded() {
        final UUID caseId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final String status = "generated";
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.nows-material-request-recorded"),
                createObjectBuilder()
                        .add("context", createObjectBuilder()
                                .add("applicationId", applicationId.toString())
                                .add("caseId", caseId.toString())
                                .add("materialId", materialId.toString())
                                .add("fileId", fileId.toString())
                                .add("hearingId", materialId.toString())
                                .add("userId", materialId.toString())
                                .add("firstClassLetter", false)
                                .add("secondClassLetter", false)
                                .add("emailNotifications", Json.createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("sendToAddress", "sendToAddress")
                                                .build())
                                        .build())
                                .build())
                        .add("status", status)
                        .build());

        nowsMaterialStatusEventProcessor.processRequestRecorded(event);

        verify(materialService).uploadMaterial(Mockito.eq(fileId), Mockito.eq(materialId), Mockito.eq(event));
    }

}
