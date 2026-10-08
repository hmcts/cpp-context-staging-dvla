package uk.gov.moj.cpp.stagingdvla.service;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentGeneratorService.DVLA_DOCUMENT_ORDER;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
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

    @Spy
    private DocumentDeliveryStatusService documentDeliveryStatusService = new DocumentDeliveryStatusService();

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
        setField(documentDeliveryStatusService, "sender", sender);
        setField(documentGeneratorService, "documentDeliveryStatusService", documentDeliveryStatusService);
        setField(documentGeneratorService, "blobContainerClient", blobContainerClient);
        setField(documentGeneratorService, "azureBlobConfiguration", azureBlobConfiguration);
    }

    @Test
    public void shouldGenerateDvlaDocument() throws Exception {
        String code = "C" ;
        final DriverNotified driverNotified = generateDriverNotified(code);

        final UUID userId = randomUUID();

        final UUID payloadFileId = randomUUID();
        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString())).thenReturn(payloadFileId);

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(nowsDocumentOrderJson1);
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor =  ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);
        documentGeneratorService.generateDvlaDocument(envelope, userId, driverNotified);
        verify(systemDocGeneratorService).generateDocument(documentGenerationRequestArgumentCaptor.capture(), eq(envelope));

        DocumentGenerationRequest request = documentGenerationRequestArgumentCaptor.getValue();
        assertThat(request.getConversionFormat(),is(ConversionFormat.PDF));
        assertThat(request.getOriginatingSource(),is("DVLADocumentOrder"));
        assertThat(request.getTemplateIdentifier(),is("EDT_DriverOutNotification"));
        assertThat(request.getSourceCorrelationId(),is(userId.toString()));
        assertThat(request.getPayloadFileServiceId(),is(payloadFileId));
    }

    @Test
    public void shouldGenerateDvlaDocumentForSjpCode() throws Exception {
        String code = "J" ;
        final DriverNotified driverNotified = generateDriverNotified(code);

        final UUID userId = randomUUID();

        final UUID payloadFileId = randomUUID();
        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString())).thenReturn(payloadFileId);

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(nowsDocumentOrderJson1);
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor =  ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);
        documentGeneratorService.generateDvlaDocument(envelope, userId, driverNotified);
        verify(systemDocGeneratorService).generateDocument(documentGenerationRequestArgumentCaptor.capture(), eq(envelope));

        DocumentGenerationRequest request = documentGenerationRequestArgumentCaptor.getValue();
        assertThat(request.getConversionFormat(),is(ConversionFormat.PDF));
        assertThat(request.getOriginatingSource(),is("DVLADocumentOrder"));
        assertThat(request.getTemplateIdentifier(),is("EDT_DriverOutNotification"));
        assertThat(request.getSourceCorrelationId(),is(userId.toString()));
        assertThat(request.getPayloadFileServiceId(),is(payloadFileId));
    }

    @Test
    public void shouldNotRequestDvlaDocumentGenerationWhenStoringThePayloadFails() throws Exception {
        final DriverNotified driverNotified = generateDriverNotified("C");

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(nowsDocumentOrderJson1);
        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString())).thenThrow(new RuntimeException("file service unavailable"));

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);

        assertDoesNotThrow(() -> documentGeneratorService.generateDvlaDocument(envelope, randomUUID(), driverNotified));

        verifyNoInteractions(systemDocGeneratorService);
    }

    @Test
    public void shouldUploadToBlobStorageAndRequestDocumentGenerationWithBlobUris() throws Exception {
        final DriverNotified driverNotified = generateDriverNotified("C");
        final String blobUrl = "https://mystorage.blob.core.windows.net/" + DVLA_DOCUMENT_ORDER + "_" + driverNotified.getMaterialId();

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn(blobUrl);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        doNothing().when(systemDocGeneratorService).generateDocumentForBlobUIR(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);

        documentGeneratorService.generateDocument(envelope, driverNotified.getMaterialId(), nowsDocumentOrderJson1, null);

        verify(fileService, never()).storePayload(any(), anyString(), anyString());
        verify(blobContainerClient).getBlobClient(DVLA_DOCUMENT_ORDER + "_" + driverNotified.getMaterialId());
        final ArgumentCaptor<BlobParallelUploadOptions> uploadOptionsCaptor = ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(uploadOptionsCaptor.capture(), eq(ofSeconds(300)), any());
        assertThat(uploadOptionsCaptor.getValue().getMetadata().get("fileName").matches(DVLA_DOCUMENT_ORDER + "_\\d{14}\\.pdf"), is(true));

        final ArgumentCaptor<DocumentGenerationRequest> requestCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        verify(systemDocGeneratorService).generateDocumentForBlobUIR(requestCaptor.capture(), eq(envelope));
        verify(systemDocGeneratorService, never()).generateDocument(any(), any());

        final DocumentGenerationRequest request = requestCaptor.getValue();
        assertThat(request.getPayloadFileServiceId(), nullValue());
        assertThat(request.getConversionFormat(), is(ConversionFormat.PDF));
        assertThat(request.getOriginatingSource(), is("DVLADocumentOrder"));
        assertThat(request.getTemplateIdentifier(), is("EDT_DriverOutNotification"));
        assertThat(request.getSourceCorrelationId(), is(driverNotified.getMaterialId().toString()));

        assertThat(request.getPayloadFileUri(), is(blobUrl));
        assertThat(request.getDestinationFileUri(), is(blobUrl + ".pdf"));

        verifyDocumentDeliveryStatusRecorded(driverNotified, DvlaDocumentDeliveryMaterialStatus.PENDING,
                blobUrl, blobUrl + ".pdf");
    }

    @Test
    public void shouldUploadDriverAuditReportToBlobStorageAndRequestCsvGeneration() {
        final UUID reportId = randomUUID();
        final String fileName = "DriverAuditReport_2026-10-08_10-10-10.csv";
        final String blobUrl = "https://mystorage.blob.core.windows.net/DriverAuditReport_" + reportId;
        final JsonObject payload = createObjectBuilder().add("driverAuditRecords", createArrayBuilder()).build();

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn(blobUrl);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-requested"), payload);
        documentGeneratorService.generateDriverAuditReportDocument(envelope, reportId, payload, fileName);

        verify(blobContainerClient).getBlobClient("DriverAuditReport_" + reportId);
        final ArgumentCaptor<BlobParallelUploadOptions> uploadOptionsCaptor = ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(uploadOptionsCaptor.capture(), eq(ofSeconds(300)), any());
        assertThat(uploadOptionsCaptor.getValue().getMetadata().get("fileName"), is(fileName));
        assertThat(uploadOptionsCaptor.getValue().getMetadata().get("templateName"), is("DvlaAuditRecords"));

        final ArgumentCaptor<DocumentGenerationRequest> requestCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        verify(systemDocGeneratorService).generateDocumentForBlobUIR(requestCaptor.capture(), eq(envelope));
        final DocumentGenerationRequest request = requestCaptor.getValue();
        assertThat(request.getOriginatingSource(), is("DvlaAuditRecords"));
        assertThat(request.getTemplateIdentifier(), is("DvlaAuditRecords"));
        assertThat(request.getConversionFormat(), is(ConversionFormat.CSV));
        assertThat(request.getSourceCorrelationId(), is(reportId.toString()));
        assertThat(request.getPayloadFileUri(), is(blobUrl));
        assertThat(request.getDestinationFileUri(), is(blobUrl + ".csv"));
    }

    @Test
    public void shouldRecordTheSjpCaseOnTheBlobPendingRecordForAnSjpCaseDocument() throws Exception {
        final DriverNotified driverNotified = generateDriverNotified("J");
        final UUID sjpCaseId = randomUUID();
        final String blobUrl = "https://mystorage.blob.core.windows.net/" + DVLA_DOCUMENT_ORDER + "_" + driverNotified.getMaterialId();

        final JsonObject nowsDocumentOrderJson = stringToJsonObjectConverter.convert(
                Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset()));

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn(blobUrl);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        doNothing().when(systemDocGeneratorService).generateDocumentForBlobUIR(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("stagingdvla.event.driver-notified"), nowsDocumentOrderJson);

        documentGeneratorService.generateDocument(envelope, driverNotified.getMaterialId(), nowsDocumentOrderJson, sjpCaseId);

        // blob name stays per material; the visible file name in metadata is the file-service one
        verify(blobContainerClient).getBlobClient(DVLA_DOCUMENT_ORDER + "_" + driverNotified.getMaterialId());
        final ArgumentCaptor<BlobParallelUploadOptions> uploadOptionsCaptor = ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(uploadOptionsCaptor.capture(), eq(ofSeconds(300)), any());
        assertThat(uploadOptionsCaptor.getValue().getMetadata().get("fileName").matches(DVLA_DOCUMENT_ORDER + "_\\d{14}\\.pdf"), is(true));
        final ArgumentCaptor<DocumentGenerationRequest> requestCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        verify(systemDocGeneratorService).generateDocumentForBlobUIR(requestCaptor.capture(), eq(envelope));
        assertThat(requestCaptor.getValue().getDestinationFileUri(), is(blobUrl + ".pdf"));

        // the SJP case rides on the PENDING record that starts tracking, so MaterialAggregate keeps the
        // blob past material SUCCESS whichever status record it processes first
        final ArgumentCaptor<Envelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> pendingRecord = envelopeArgumentCaptor.getValue();
        assertThat(pendingRecord.payload().getString("materialStatus"), is("PENDING"));
        assertThat(pendingRecord.payload().getString("payloadBlobUri"), is(blobUrl));
        assertThat(pendingRecord.payload().getString("caseId"), is(sjpCaseId.toString()));
        assertThat(pendingRecord.payload().containsKey("sjpStatus"), is(false));
    }

    @Test
    public void shouldThrowExceptionAndNotRequestDocumentGenerationWhenBlobUploadFails() throws Exception {
        final DriverNotified driverNotified = generateDriverNotified("C");

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(azureBlobConfiguration.getTransferTimeout()).thenReturn(ofSeconds(300));
        when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any()))
                .thenThrow(new RuntimeException("blob storage unavailable"));

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);

        assertThrows(RuntimeException.class, () -> documentGeneratorService.generateDocument(envelope, driverNotified.getMaterialId(), nowsDocumentOrderJson1, null));

        verifyNoInteractions(systemDocGeneratorService);
        verify(sender, never()).sendAsAdmin(any());
    }

    private void verifyDocumentDeliveryStatusRecorded(final DriverNotified driverNotified, final DvlaDocumentDeliveryMaterialStatus expectedStatus,
                                                      final String expectedPayloadFileUri, final String expectedDestinationFileUri) {
        final ArgumentCaptor<Envelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> capturedEnvelope = envelopeArgumentCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is(DocumentDeliveryStatusService.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY));
        assertThat(capturedEnvelope.payload().getString("materialId"), is(driverNotified.getMaterialId().toString()));
        assertThat(capturedEnvelope.payload().getString("materialStatus"), is(expectedStatus.name()));
        assertThat(capturedEnvelope.payload().getString("payloadBlobUri", null), is(expectedPayloadFileUri));
        assertThat(capturedEnvelope.payload().getString("documentBlobUri", null), is(expectedDestinationFileUri));
        assertThat(capturedEnvelope.payload().containsKey("caseId"), is(false));
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
