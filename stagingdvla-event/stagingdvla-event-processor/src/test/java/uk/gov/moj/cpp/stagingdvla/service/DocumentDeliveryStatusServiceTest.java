package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.FAILED;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.PENDING;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery.material;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery.sjpCase;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentDeliveryStatusServiceTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private final Metadata originatingMetadata = metadataWithRandomUUID("public.systemdocgenerator.events.document-available").build();

    @Test
    public void shouldRecordMaterialStatusWithoutOptionalFields() {
        final UUID materialId = randomUUID();

        documentDeliveryStatusService.record(originatingMetadata, material(materialId, FAILED));

        final Envelope<JsonObject> envelope = capturedEnvelope();
        assertThat(envelope.metadata().name(), is(STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY));
        assertThat(envelope.metadata().id(), is(originatingMetadata.id()));
        assertThat(envelope.payload().getString("materialId"), is(materialId.toString()));
        assertThat(envelope.payload().getString("materialStatus"), is("FAILED"));
        assertThat(envelope.payload().size(), is(2));
    }

    @Test
    public void shouldRecordMaterialStatusWithBlobUris() {
        final UUID materialId = randomUUID();
        final String payloadBlobUri = "http://cpp-azurite:10000/devstoreaccount1/stagingdvla-files/internal%2FDVLADocumentOrder";
        final String documentBlobUri = payloadBlobUri + ".pdf";

        documentDeliveryStatusService.record(originatingMetadata, material(materialId, PENDING, payloadBlobUri, documentBlobUri));

        final JsonObject payload = capturedEnvelope().payload();
        assertThat(payload.getString("materialId"), is(materialId.toString()));
        assertThat(payload.getString("materialStatus"), is("PENDING"));
        assertThat(payload.getString("payloadBlobUri"), is(payloadBlobUri));
        assertThat(payload.getString("documentBlobUri"), is(documentBlobUri));
        assertThat(payload.size(), is(4));
    }

    @Test
    public void shouldRecordSjpCaseStatus() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();

        documentDeliveryStatusService.record(originatingMetadata, sjpCase(materialId, caseId, sjpCorrelationId, PENDING));

        final JsonObject payload = capturedEnvelope().payload();
        assertThat(payload.getString("materialId"), is(materialId.toString()));
        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.getString("sjpCorrelationId"), is(sjpCorrelationId.toString()));
        assertThat(payload.getString("sjpStatus"), is("PENDING"));
        assertThat(payload.containsKey("materialStatus"), is(false));
        assertThat(payload.size(), is(4));
    }

    private Envelope<JsonObject> capturedEnvelope() {
        verify(sender).sendAsAdmin(envelopeCaptor.capture());
        return envelopeCaptor.getValue();
    }
}
