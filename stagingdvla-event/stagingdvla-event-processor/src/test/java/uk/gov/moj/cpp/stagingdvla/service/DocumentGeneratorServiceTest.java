package uk.gov.moj.cpp.stagingdvla.service;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentGeneratorService.DVLA_DOCUMENT_ORDER;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentGeneratorService.DVLA_DOCUMENT_TEMPLATE_NAME;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.blobstore.AzureFileStoreBlobConfiguration;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus;
import java.util.Collections;
import java.util.UUID;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import javax.json.JsonObject;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentGeneratorServiceTest {

    @InjectMocks
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private SystemDocGeneratorService systemDocGeneratorService;

    @Mock
    private FileService fileService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Sender sender;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private AzureFileStoreBlobConfiguration azureBlobConfiguration;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static final UUID ORDER_HEARING_ID = randomUUID();
    private static final String COURT_CENTER_NAME = "Liverpool Crown Court";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String SJP_UPLOAD_CASE_DOCUMENT = "sjp.upload-case-document" ;

    @BeforeEach
    public void setUp() {
        setField(documentGeneratorService, "sender", sender);
        setField(documentGeneratorService, "featureControlGuard", featureControlGuard);
        setField(documentGeneratorService, "blobContainerClient", blobContainerClient);
        setField(documentGeneratorService, "azureBlobConfiguration", azureBlobConfiguration);
    }

    @Test
    public void shouldGenerateDocument() throws Exception {
        String code = "C" ;
        final DriverNotified driverNotified = generateDriverNotified(code);

        final UUID payloadFileId = randomUUID();
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(true);
        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString(), any(ConversionFormat.class))).thenReturn(payloadFileId);

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor =  ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);
        documentGeneratorService.generateDocument(envelope, driverNotified.getMaterialId(), nowsDocumentOrderJson1,
                documentGeneratorService.getMaterialIdAmendedFileName(DVLA_DOCUMENT_ORDER, driverNotified.getMaterialId().toString()),
                DVLA_DOCUMENT_TEMPLATE_NAME,
                ConversionFormat.PDF,
                DVLA_DOCUMENT_ORDER);
        verify(systemDocGeneratorService).generateDocument(documentGenerationRequestArgumentCaptor.capture(), eq(envelope));

        DocumentGenerationRequest request = documentGenerationRequestArgumentCaptor.getValue();
        assertThat(request.getConversionFormat(),is(ConversionFormat.PDF));
        assertThat(request.getOriginatingSource(),is("DVLADocumentOrder"));
        assertThat(request.getTemplateIdentifier(),is("EDT_DriverOutNotification"));
        assertThat(request.getSourceCorrelationId(),is(driverNotified.getMaterialId().toString()));
        assertThat(request.getPayloadFileServiceId(),is(payloadFileId));

        verifyDocumentDeliveryStatusRecorded(driverNotified, DvlaDocumentDeliveryMaterialStatus.PENDING);
    }

    @Test
    public void shouldGenerateDocumentForSjpCode() throws Exception {
        String code = "J" ;
        final DriverNotified driverNotified = generateDriverNotified(code);

        final UUID payloadFileId = randomUUID();
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(true);
        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString(), any(ConversionFormat.class))).thenReturn(payloadFileId);

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor =  ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);
        documentGeneratorService.generateDocument(envelope, driverNotified.getMaterialId(), nowsDocumentOrderJson1,
                documentGeneratorService.getMaterialIdAmendedFileName(DVLA_DOCUMENT_ORDER, driverNotified.getMaterialId().toString()),
                DVLA_DOCUMENT_TEMPLATE_NAME,
                ConversionFormat.PDF,
                DVLA_DOCUMENT_ORDER);
        verify(systemDocGeneratorService).generateDocument(documentGenerationRequestArgumentCaptor.capture(), eq(envelope));

        DocumentGenerationRequest request = documentGenerationRequestArgumentCaptor.getValue();
        assertThat(request.getConversionFormat(),is(ConversionFormat.PDF));
        assertThat(request.getOriginatingSource(),is("DVLADocumentOrder"));
        assertThat(request.getTemplateIdentifier(),is("EDT_DriverOutNotification"));
        assertThat(request.getSourceCorrelationId(),is(driverNotified.getMaterialId().toString()));
        assertThat(request.getPayloadFileServiceId(),is(payloadFileId));

        verifyDocumentDeliveryStatusRecorded(driverNotified, DvlaDocumentDeliveryMaterialStatus.PENDING);
    }

    @Test
    public void shouldUploadToBlobStorageAndRequestDocumentGenerationWithBlobUrisWhenFeatureDisabled() throws Exception {
        final DriverNotified driverNotified = generateDriverNotified("C");
        final String blobUrl = "https://mystorage.blob.core.windows.net/internal/" + driverNotified.getMaterialId();

        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn(blobUrl);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);

        final String fileName = documentGeneratorService.getMaterialIdAmendedFileName(DVLA_DOCUMENT_ORDER, driverNotified.getMaterialId().toString());
        documentGeneratorService.generateDocument(envelope, driverNotified.getMaterialId(), nowsDocumentOrderJson1,
                fileName,
                DVLA_DOCUMENT_TEMPLATE_NAME,
                ConversionFormat.PDF,
                DVLA_DOCUMENT_ORDER);

        verify(fileService, never()).storePayload(any(), anyString(), anyString(), any(ConversionFormat.class));
        verify(blobContainerClient).getBlobClient("internal/" + fileName.replaceAll("\\.[^.]*$", ""));
        verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), eq(ofSeconds(300)), any());

        final ArgumentCaptor<DocumentGenerationRequest> requestCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        verify(systemDocGeneratorService).generateDocument(requestCaptor.capture(), eq(envelope));

        final DocumentGenerationRequest request = requestCaptor.getValue();
        assertThat(request.getPayloadFileServiceId(), nullValue());
        assertThat(request.getConversionFormat(), is(ConversionFormat.PDF));
        assertThat(request.getOriginatingSource(), is("DVLADocumentOrder"));
        assertThat(request.getTemplateIdentifier(), is("EDT_DriverOutNotification"));
        assertThat(request.getSourceCorrelationId(), is(driverNotified.getMaterialId().toString()));

        assertThat(request.getPayloadFileUri(), is(blobUrl));
        assertThat(request.getDestinationFileUri(), is(blobUrl + "." + fileName.replaceAll(".*\\.", "")));

        verifyDocumentDeliveryStatusRecorded(driverNotified, DvlaDocumentDeliveryMaterialStatus.PENDING);
    }

    @Test
    public void shouldSwallowExceptionAndNotRequestDocumentGenerationWhenBlobUploadFails() throws Exception {
        final DriverNotified driverNotified = generateDriverNotified("C");

        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any()))
                .thenThrow(new RuntimeException("blob storage unavailable"));

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);

        documentGeneratorService.generateDocument(envelope, driverNotified.getMaterialId(), nowsDocumentOrderJson1,
                documentGeneratorService.getMaterialIdAmendedFileName(DVLA_DOCUMENT_ORDER, driverNotified.getMaterialId().toString()),
                DVLA_DOCUMENT_TEMPLATE_NAME,
                ConversionFormat.PDF,
                DVLA_DOCUMENT_ORDER);

        verifyNoInteractions(systemDocGeneratorService);
        verify(sender, never()).sendAsAdmin(any());
    }

    private void verifyDocumentDeliveryStatusRecorded(final DriverNotified driverNotified, final DvlaDocumentDeliveryMaterialStatus expectedStatus) {
        final ArgumentCaptor<Envelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> capturedEnvelope = envelopeArgumentCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is(DocumentGeneratorService.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY));
        assertThat(capturedEnvelope.payload().getString("materialId"), is(driverNotified.getMaterialId().toString()));
        assertThat(capturedEnvelope.payload().getString("materialStatus"), is(expectedStatus.name()));
    }

    public static DriverNotified generateDriverNotified(String initiationCode) {
        DriverNotified driverNotified = DriverNotified.driverNotified()
                .withOrderingCourt(CourtCentre.courtCentre()
                        .withName(COURT_CENTER_NAME).build())
                .withOrderingHearingId(ORDER_HEARING_ID)
                .withMaterialId(randomUUID())
                .withCases(Collections.singletonList(Cases.cases().withCaseId(randomUUID()).withInitiationCode(initiationCode).build()))
                .build();

        return driverNotified;
    }


}
