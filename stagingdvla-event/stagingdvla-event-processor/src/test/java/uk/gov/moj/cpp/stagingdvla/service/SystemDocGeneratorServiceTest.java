package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemDocGeneratorServiceTest {

    @InjectMocks
    private SystemDocGeneratorService systemDocGeneratorService;

    @Mock
    private Sender sender;

    @Mock
    private DocumentGenerationRequest request;

    @Mock
    private JsonEnvelope envelope;

    @Test
    void shouldSendTheGenerateDocumentCommand() {
        when(request.getOriginatingSource()).thenReturn("stagingdvla");
        when(request.getTemplateIdentifier()).thenReturn("template-1");
        when(request.getConversionFormat()).thenReturn(ConversionFormat.PDF);
        when(request.getSourceCorrelationId()).thenReturn("correlation-1");
        when(request.getPayloadFileServiceId()).thenReturn(randomUUID());
        when(envelope.metadata()).thenReturn(metadataWithRandomUUID("stagingdvla.event").build());

        systemDocGeneratorService.generateDocument(request, envelope);

        verify(sender).sendAsAdmin(any(Envelope.class));
    }
}
