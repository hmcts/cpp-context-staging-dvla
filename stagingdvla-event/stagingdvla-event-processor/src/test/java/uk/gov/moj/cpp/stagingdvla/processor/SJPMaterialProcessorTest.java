package uk.gov.moj.cpp.stagingdvla.processor;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.FAILED;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.SUCCESS;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;
import uk.gov.moj.cpp.persistence.repository.DvlaDocumentDeliveryRepository;
import uk.gov.moj.cpp.stagingdvla.blobstore.AzureFileStoreBlobConfiguration;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService.DocumentDelivery;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SJPMaterialProcessorTest {

    private static final String CASE_DOCUMENT_ADDED = "public.sjp.case-document-added";
    private static final String CASE_DOCUMENT_ADDITION_FAILED = "public.sjp.case-document-addition-failed";
    private static final String CASE_DOCUMENT_UPLOAD_REJECTED = "public.sjp.events.case-document-upload-rejected";
    private static final String CONTAINER_NAME = "stagingdvla-files";
    private static final String DOCUMENT_URI = "https://sadevfilestore.blob.core.windows.net/" + CONTAINER_NAME + "/generated/notification.pdf";
    // Azurite (local/IT) addresses the account in the path rather than the host
    private static final String AZURITE_DOCUMENT_URI = "http://cpp-azurite:10000/devstoreaccount1/" + CONTAINER_NAME + "/internal%2FDVLADocumentOrder_1.pdf";
    private static final String OTHER_CONTAINER_DOCUMENT_URI = "https://sadevfilestore.blob.core.windows.net/sjp-files/generated/notification.pdf";

    @Mock
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    @Mock
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Mock
    private AzureFileStoreBlobConfiguration azureFileStoreBlobConfiguration;

    @InjectMocks
    private SJPMaterialProcessor sjpMaterialProcessor;

    @Captor
    private ArgumentCaptor<DocumentDelivery> documentDeliveryCaptor;

    private final UUID caseId = randomUUID();
    private final UUID caseDocumentId = randomUUID();
    private final UUID materialId = randomUUID();

    @BeforeEach
    public void setUp() {
        lenient().when(azureFileStoreBlobConfiguration.getContainerName()).thenReturn(CONTAINER_NAME);
    }

    @Test
    public void shouldRecordSjpSuccessForMatchingDelivery() {
        when(dvlaDocumentDeliveryRepository.findByDocumentBlobUri(DOCUMENT_URI))
                .thenReturn(singletonList(delivery()));

        sjpMaterialProcessor.handleCaseDocumentAddedEvent(caseDocumentAdded(DOCUMENT_URI));

        assertRecordedSjpStatus(SUCCESS);
    }

    @Test
    public void shouldThrowWhenNoDeliveryMatchesOwnDocument() {
        when(dvlaDocumentDeliveryRepository.findByDocumentBlobUri(DOCUMENT_URI)).thenReturn(emptyList());

        final JsonEnvelope event = caseDocumentAdded(DOCUMENT_URI);
        assertThrows(RuntimeException.class, () -> sjpMaterialProcessor.handleCaseDocumentAddedEvent(event));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreWhenDocumentUriAbsent() {
        sjpMaterialProcessor.handleCaseDocumentAddedEvent(caseDocumentAdded(null));

        verifyNoInteractions(dvlaDocumentDeliveryRepository, documentDeliveryStatusService);
    }

    @Test
    public void shouldRecordSjpSuccessForAzuriteDocumentUri() {
        when(dvlaDocumentDeliveryRepository.findByDocumentBlobUri(AZURITE_DOCUMENT_URI)).thenReturn(singletonList(delivery()));

        sjpMaterialProcessor.handleCaseDocumentAddedEvent(caseDocumentAdded(AZURITE_DOCUMENT_URI));

        assertRecordedSjpStatus(SUCCESS);
    }

    @Test
    public void shouldIgnoreWhenDocumentUriInOtherContainer() {
        sjpMaterialProcessor.handleCaseDocumentAddedEvent(caseDocumentAdded(OTHER_CONTAINER_DOCUMENT_URI));

        verifyNoInteractions(dvlaDocumentDeliveryRepository, documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreWhenDocumentUriNotABlobUrl() {
        sjpMaterialProcessor.handleCaseDocumentAddedEvent(caseDocumentAdded("not a url"));

        verifyNoInteractions(dvlaDocumentDeliveryRepository, documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreCaseDocumentAdditionFailedWhenDocumentUriInOtherContainer() {
        sjpMaterialProcessor.handleCaseDocumentFailedEvent(caseDocumentAdditionFailed(OTHER_CONTAINER_DOCUMENT_URI));

        verifyNoInteractions(dvlaDocumentDeliveryRepository, documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreCaseDocumentUploadRejectedWhenDocumentReferenceUriInOtherContainer() {
        sjpMaterialProcessor.handleCaseDocumentRejectedEvent(caseDocumentUploadRejected(OTHER_CONTAINER_DOCUMENT_URI));

        verifyNoInteractions(dvlaDocumentDeliveryRepository, documentDeliveryStatusService);
    }

    @Test
    public void shouldRecordSjpFailedWhenCaseDocumentAdditionFailed() {
        when(dvlaDocumentDeliveryRepository.findByDocumentBlobUri(DOCUMENT_URI))
                .thenReturn(singletonList(delivery()));

        sjpMaterialProcessor.handleCaseDocumentFailedEvent(caseDocumentAdditionFailed(DOCUMENT_URI));

        assertRecordedSjpStatus(SUCCESS);
    }

    @Test
    public void shouldThrowWhenCaseDocumentAdditionFailedMatchesNoDelivery() {
        when(dvlaDocumentDeliveryRepository.findByDocumentBlobUri(DOCUMENT_URI)).thenReturn(emptyList());

        final JsonEnvelope event = caseDocumentAdditionFailed(DOCUMENT_URI);
        assertThrows(RuntimeException.class, () -> sjpMaterialProcessor.handleCaseDocumentFailedEvent(event));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreCaseDocumentAdditionFailedWhenDocumentUriAbsent() {
        sjpMaterialProcessor.handleCaseDocumentFailedEvent(caseDocumentAdditionFailed(null));

        verifyNoInteractions(dvlaDocumentDeliveryRepository, documentDeliveryStatusService);
    }

    @Test
    public void shouldRecordSjpFailedWhenCaseDocumentUploadRejected() {
        when(dvlaDocumentDeliveryRepository.findByDocumentBlobUri(DOCUMENT_URI)).thenReturn(singletonList(delivery()));

        sjpMaterialProcessor.handleCaseDocumentRejectedEvent(caseDocumentUploadRejected(DOCUMENT_URI));

        assertRecordedSjpStatus(FAILED);
    }

    @Test
    public void shouldThrowWhenCaseDocumentUploadRejectedMatchesNoDelivery() {
        when(dvlaDocumentDeliveryRepository.findByDocumentBlobUri(DOCUMENT_URI)).thenReturn(emptyList());

        final JsonEnvelope event = caseDocumentUploadRejected(DOCUMENT_URI);
        assertThrows(RuntimeException.class, () -> sjpMaterialProcessor.handleCaseDocumentRejectedEvent(event));

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    public void shouldIgnoreCaseDocumentUploadRejectedWhenDocumentReferenceUriAbsent() {
        sjpMaterialProcessor.handleCaseDocumentRejectedEvent(caseDocumentUploadRejected(null));

        verifyNoInteractions(dvlaDocumentDeliveryRepository, documentDeliveryStatusService);
    }

    private DvlaDocumentDeliveryEntity delivery() {
        return new DvlaDocumentDeliveryEntity(materialId, now(), "SUCCESS", "payload/blob/uri", DOCUMENT_URI,
                caseId, caseDocumentId, "PENDING");
    }

    private void assertRecordedSjpStatus(final DvlaDocumentDeliveryMaterialStatus sjpStatus) {
        verify(documentDeliveryStatusService).record(any(), documentDeliveryCaptor.capture());
        final DocumentDelivery documentDelivery = documentDeliveryCaptor.getValue();
        assertThat(documentDelivery.materialId(), is(materialId));
        assertThat(documentDelivery.caseId(), is(caseId));
        assertThat(documentDelivery.sjpCorrelationId(), is(caseDocumentId));
        assertThat(documentDelivery.sjpStatus(), is(sjpStatus));
        assertThat(documentDelivery.materialStatus(), is(nullValue()));
    }

    private JsonEnvelope caseDocumentAdded(final String documentUri) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("id", caseDocumentId.toString())
                .add("materialId", randomUUID().toString())
                .add("documentType", "ELECTRONIC_NOTIFICATIONS");
        addIfPresent(payload, "documentUri", documentUri);
        return envelopeFrom(metadataWithRandomUUID(CASE_DOCUMENT_ADDED), payload.build());
    }

    private JsonEnvelope caseDocumentAdditionFailed(final String documentUri) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("documentId", caseDocumentId.toString())
                .add("description", "Add Case Document");
        addIfPresent(payload, "documentUri", documentUri);
        return envelopeFrom(metadataWithRandomUUID(CASE_DOCUMENT_ADDITION_FAILED), payload.build());
    }

    private JsonEnvelope caseDocumentUploadRejected(final String documentReferenceUri) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("description", "Case Document Upload rejected as case is referred to court for hearing");
        if (documentReferenceUri != null) {
            payload.add("documentReferenceUri", documentReferenceUri);
        } else {
            payload.add("documentId", caseDocumentId.toString());
        }
        return envelopeFrom(metadataWithRandomUUID(CASE_DOCUMENT_UPLOAD_REJECTED), payload.build());
    }

    private static void addIfPresent(final JsonObjectBuilder payload, final String key, final String value) {
        if (value != null) {
            payload.add(key, value);
        }
    }
}
